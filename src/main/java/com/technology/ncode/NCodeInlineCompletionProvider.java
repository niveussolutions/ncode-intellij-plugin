package com.technology.ncode;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides inline code completion functionality using Vertex AI.
 * Monitors typing and displays AI-generated code suggestions after a debounce
 * period.
 */
public class NCodeInlineCompletionProvider extends TypedHandlerDelegate implements Disposable {
    private static final Logger LOG = Logger.getInstance(NCodeInlineCompletionProvider.class);

    // Configuration constants
    private static final int DEBOUNCE_DELAY_MS = 1000;
    private static final int TOP_CONTEXT_LINES = 10;
    private static final int BOTTOM_CONTEXT_LINES = 5;
    private static final int HIGHLIGHTING_ALPHA = 40;
    private static final int FOREGROUND_ALPHA = 180;
    private static final int FALLBACK_BG_RED = 200;
    private static final int FALLBACK_BG_GREEN = 200;
    private static final int FALLBACK_BG_BLUE = 200;

    // Use IntelliJ's executor service for better integration with the platform
    private final ScheduledExecutorService scheduler = AppExecutorUtil.getAppScheduledExecutorService();
    private final AtomicReference<ScheduledFuture<?>> pendingTask = new AtomicReference<>();

    // Completion state
    private CompletionState completionState;

    // We're using static inner classes to improve memory efficiency
    private static class CompletionState {
        final String text;
        final int offset;
        final RangeHighlighter highlighter;
        final AnAction tabAction;
        final AnAction escapeAction;

        CompletionState(String text, int offset, RangeHighlighter highlighter,
                AnAction tabAction, AnAction escapeAction) {
            this.text = text;
            this.offset = offset;
            this.highlighter = highlighter;
            this.tabAction = tabAction;
            this.escapeAction = escapeAction;
        }

        void cleanup(Editor editor) {
            if (highlighter != null && highlighter.isValid()) {
                editor.getMarkupModel().removeHighlighter(highlighter);
            }

            if (tabAction != null) {
                tabAction.unregisterCustomShortcutSet(editor.getContentComponent());
            }

            if (escapeAction != null) {
                escapeAction.unregisterCustomShortcutSet(editor.getContentComponent());
            }
        }
    }

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        // Process all character typing events with debounce
        debounce(() -> processCompletion(project, editor), DEBOUNCE_DELAY_MS);
        return Result.CONTINUE;
    }

    private void processCompletion(Project project, Editor editor) {
        // Avoid processing if editor is disposed or project is closed
        if (project.isDisposed() || editor.isDisposed()) {
            return;
        }

        // Schedule modifications on the EDT and wrap them in a write action
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || editor.isDisposed()) {
                return;
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    // Clean up any existing completion state
                    cleanupCurrentCompletion(editor);

                    // Get the current caret offset
                    int offset = editor.getCaretModel().getOffset();

                    // Get the surrounding lines text
                    String surroundingLines = getSurroundingLines(editor);
                    LOG.debug("Processing completion with surrounding lines: " + surroundingLines);

                    generateCompletion(editor, offset, surroundingLines);
                } catch (Exception e) {
                    LOG.error("Error processing completion", e);
                    cleanupCurrentCompletion(editor);
                }
            });
        });
    }

    private void generateCompletion(Editor editor, int offset, String surroundingLines) {
        // Skip if we're already showing a completion
        if (completionState != null) {
            return;
        }

        InlineVertexAi inlineVertexAi = new InlineVertexAi();
        try {
            GenerateContentResponse response = inlineVertexAi.generateContent(surroundingLines);
            String generatedText = InlineVertexAi.extractGeneratedText(response);

            if (generatedText != null && !generatedText.isEmpty()) {
                // Insert the completion text at the caret
                Document document = editor.getDocument();
                document.insertString(offset, generatedText);

                // Apply transparent highlighting
                RangeHighlighter highlighter = applyTransparentHighlighting(editor, offset,
                        offset + generatedText.length());

                // Install custom actions for tab and escape
                AnAction tabAction = installTabCompletionAction(editor, offset, generatedText);
                AnAction escapeAction = installEscapeAction(editor, offset, generatedText);

                // Store the completion state
                completionState = new CompletionState(
                        generatedText,
                        offset,
                        highlighter,
                        tabAction,
                        escapeAction);
            } else {
                LOG.warn("No valid completion generated. Raw response: " + response);
            }
        } catch (IOException e) {
            LOG.warn("Error calling Vertex AI", e);
        } catch (Exception e) {
            LOG.error("Unexpected error during completion generation", e);
        }
    }

    private void cleanupCurrentCompletion(Editor editor) {
        Optional.ofNullable(completionState).ifPresent(state -> {
            state.cleanup(editor);
            completionState = null;
        });
    }

    private RangeHighlighter applyTransparentHighlighting(Editor editor, int startOffset, int endOffset) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        TextAttributes attributes = new TextAttributes();

        // Get the actual selection background color from the scheme
        Color selectionColor = scheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR);
        if (selectionColor != null) {
            // Make a more transparent version of the selection color
            attributes.setBackgroundColor(new Color(
                    selectionColor.getRed(),
                    selectionColor.getGreen(),
                    selectionColor.getBlue(),
                    HIGHLIGHTING_ALPHA));
        } else {
            // Fallback color if selection color is not defined
            attributes.setBackgroundColor(new Color(
                    FALLBACK_BG_RED,
                    FALLBACK_BG_GREEN,
                    FALLBACK_BG_BLUE,
                    HIGHLIGHTING_ALPHA));
        }

        // Set a subtle foreground color
        Color defaultForeground = scheme.getDefaultForeground();
        if (defaultForeground != null) {
            attributes.setForegroundColor(new Color(
                    defaultForeground.getRed(),
                    defaultForeground.getGreen(),
                    defaultForeground.getBlue(),
                    FOREGROUND_ALPHA));
        }

        return editor.getMarkupModel().addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                attributes,
                HighlighterTargetArea.EXACT_RANGE);
    }

    private AnAction installTabCompletionAction(Editor editor, int offset, String generatedText) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction defaultTabAction = actionManager.getAction(IdeActions.ACTION_EDITOR_TAB);

        AnAction tabAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Calculate the position at the end of the generated text
                int endPosition = offset + generatedText.length();

                // Move the caret to the end of the generated text
                editor.getCaretModel().moveToOffset(endPosition);

                // Clean up
                cleanupCurrentCompletion(editor);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        };

        tabAction.registerCustomShortcutSet(
                defaultTabAction.getShortcutSet(),
                editor.getContentComponent());

        return tabAction;
    }

    private AnAction installEscapeAction(Editor editor, int offset, String generatedText) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction defaultEscapeAction = actionManager.getAction(IdeActions.ACTION_EDITOR_ESCAPE);

        AnAction escapeAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Project project = editor.getProject();
                if (project == null || project.isDisposed()) {
                    return;
                }

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        Document document = editor.getDocument();
                        int endOffset = offset + generatedText.length();
                        if (endOffset <= document.getTextLength()) {
                            document.deleteString(offset, endOffset);
                        }
                    } catch (Exception ex) {
                        LOG.error("Error removing completion text", ex);
                    } finally {
                        cleanupCurrentCompletion(editor);
                    }
                });
            }
        };

        escapeAction.registerCustomShortcutSet(
                Objects.requireNonNull(defaultEscapeAction).getShortcutSet(),
                editor.getContentComponent());

        return escapeAction;
    }

    private String getSurroundingLines(Editor editor) {
        CaretModel caretModel = editor.getCaretModel();
        int currentLine = caretModel.getLogicalPosition().line;
        int currentOffset = caretModel.getOffset();
        Document document = editor.getDocument();
        int lineCount = document.getLineCount();

        if (lineCount == 0) {
            return "";
        }

        int startLine = Math.max(0, currentLine - TOP_CONTEXT_LINES);
        int endLine = Math.min(lineCount - 1, currentLine + BOTTOM_CONTEXT_LINES);

        StringBuilder lines = new StringBuilder(1024); // Pre-allocate buffer size
        for (int i = startLine; i <= endLine; i++) {
            int lineStartOffset = document.getLineStartOffset(i);
            int lineEndOffset = document.getLineEndOffset(i);
            String lineText = document.getText(new TextRange(lineStartOffset, lineEndOffset));

            if (i == currentLine) {
                int caretPositionInLine = currentOffset - lineStartOffset;
                if (caretPositionInLine >= 0 && caretPositionInLine <= lineText.length()) {
                    lines.append(lineText, 0, caretPositionInLine)
                            .append("{caret is here}")
                            .append(lineText.substring(caretPositionInLine));
                } else {
                    lines.append(lineText);
                }
            } else {
                lines.append(lineText);
            }

            lines.append('\n');
        }
        return lines.toString();
    }

    /**
     * Debounce a given task so that rapid key events cancel previous tasks.
     * After DEBOUNCE_DELAY_MS milliseconds of inactivity, the task will be
     * executed.
     */
    private void debounce(Runnable task, int delayMs) {
        ScheduledFuture<?> oldTask = pendingTask.getAndSet(null);
        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        pendingTask.set(scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> task = pendingTask.getAndSet(null);
        if (task != null) {
            task.cancel(true);
        }

        // No need to shutdown the scheduler as it's provided by the platform

        // Ensure we clean up any UI components
        completionState = null;
    }
}