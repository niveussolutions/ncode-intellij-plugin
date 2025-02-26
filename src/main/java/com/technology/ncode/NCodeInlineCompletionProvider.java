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
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NCodeInlineCompletionProvider extends TypedHandlerDelegate implements Disposable {
    private static final Logger LOGGER = Logger.getLogger(NCodeInlineCompletionProvider.class.getName());
    private static final int DEBOUNCE_DELAY_MS = 1000;
    private static final int TOP_CONTEXT_LINES = 10;
    private static final int BOTTOM_CONTEXT_LINES = 5;
    private static final int HIGHLIGHTING_ALPHA = 40;
    private static final int FOREGROUND_ALPHA = 180;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "NCodeCompletionThread");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> pendingTask;

    private AnAction currentTabAction;
    private AnAction currentEscapeAction;
    private RangeHighlighter currentHighlighter;

    private String generatedTextCache;
    private int generatedTextOffset = -1;

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        debounce(() -> processCompletion(project, editor), DEBOUNCE_DELAY_MS);
        return Result.CONTINUE;
    }

    private void processCompletion(Project project, Editor editor) {
        // Schedule modifications on the EDT and wrap them in a write action
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    // Get the current caret offset
                    int offset = editor.getCaretModel().getOffset();

                    // Get the surrounding lines text
                    String surroundingLines = getSurroundingLines(editor);
                    LOGGER.info("Processing completion with surrounding lines: " + surroundingLines);

                    generateCompletion(editor, offset, surroundingLines);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing completion", e);
                    clearCache(editor);
                }
            });
        });
    }

    private void generateCompletion(Editor editor, int offset, String surroundingLines) {
        InlineVertexAi inlineVertexAi = new InlineVertexAi();
        try {
            GenerateContentResponse response = inlineVertexAi.generateContent(surroundingLines);
            String generatedText = InlineVertexAi.extractGeneratedText(response);

            if (generatedText != null && !generatedText.isEmpty()) {
                // Store the generated text and offset
                generatedTextCache = generatedText;
                generatedTextOffset = offset;

                // Insert the completion text at the caret
                editor.getDocument().insertString(offset, generatedText);

                // Apply transparent highlighting
                applyTransparentHighlighting(editor, offset, offset + generatedText.length());

                // Install custom actions for tab and escape
                installTabCompletionAction(editor);
                installEscapeAction(editor);
            } else {
                LOGGER.warning("No valid completion generated. Raw response: " + response);
                clearCache(editor);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error calling Vertex AI", e);
            clearCache(editor);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during completion generation", e);
            clearCache(editor);
        }
    }

    private void clearCache(Editor editor) {
        generatedTextCache = null;
        generatedTextOffset = -1;
        removeHighlighting(editor);
    }

    private void applyTransparentHighlighting(Editor editor, int startOffset, int endOffset) {
        if (currentHighlighter != null) {
            removeHighlighting(editor);
        }

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
            attributes.setBackgroundColor(new Color(200, 200, 200, HIGHLIGHTING_ALPHA));
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

        MarkupModel markupModel = editor.getMarkupModel();
        currentHighlighter = markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                attributes,
                HighlighterTargetArea.EXACT_RANGE);
    }

    private void removeHighlighting(Editor editor) {
        if (currentHighlighter != null && currentHighlighter.isValid()) {
            editor.getMarkupModel().removeHighlighter(currentHighlighter);
            currentHighlighter = null;
        }
    }

    private void installTabCompletionAction(Editor editor) {
        // Remove previous action if exists
        if (currentTabAction != null) {
            currentTabAction.unregisterCustomShortcutSet(editor.getContentComponent());
        }

        ActionManager actionManager = ActionManager.getInstance();
        AnAction defaultTabAction = actionManager.getAction(IdeActions.ACTION_EDITOR_TAB);

        currentTabAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (generatedTextCache != null && generatedTextOffset != -1) {
                    // Calculate the position at the end of the generated text
                    int endPosition = generatedTextOffset + generatedTextCache.length();

                    // Move the caret to the end of the generated text
                    editor.getCaretModel().moveToOffset(endPosition);

                    // Remove highlighting and clear cache
                    clearCache(editor);
                } else {
                    // If no suggestion is active, perform the default tab action
                    defaultTabAction.actionPerformed(e);
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                // Enable this action only when there's a suggestion or to allow default
                // behavior
                e.getPresentation().setEnabled(true);
            }
        };

        currentTabAction.registerCustomShortcutSet(
                defaultTabAction.getShortcutSet(),
                editor.getContentComponent());
    }

    private void installEscapeAction(Editor editor) {
        // Remove previous action if exists
        if (currentEscapeAction != null) {
            currentEscapeAction.unregisterCustomShortcutSet(editor.getContentComponent());
        }

        ActionManager actionManager = ActionManager.getInstance();
        AnAction defaultEscapeAction = actionManager.getAction(IdeActions.ACTION_EDITOR_ESCAPE);

        currentEscapeAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (generatedTextCache != null && generatedTextOffset != -1) {
                    Project project = editor.getProject();
                    if (project != null) {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                Document document = editor.getDocument();
                                int endOffset = generatedTextOffset + generatedTextCache.length();
                                if (endOffset <= document.getTextLength()) {
                                    document.deleteString(generatedTextOffset, endOffset);
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.SEVERE, "Error removing completion text", ex);
                            } finally {
                                clearCache(editor);
                            }
                        });
                    }
                } else if (defaultEscapeAction != null) {
                    defaultEscapeAction.actionPerformed(e);
                }
            }
        };

        currentEscapeAction.registerCustomShortcutSet(
                defaultEscapeAction.getShortcutSet(),
                editor.getContentComponent());
    }

    private String getSurroundingLines(Editor editor) {
        CaretModel caretModel = editor.getCaretModel();
        int currentLine = caretModel.getLogicalPosition().line;
        int currentOffset = caretModel.getOffset();
        Document document = editor.getDocument();
        int startLine = Math.max(0, currentLine - TOP_CONTEXT_LINES);
        int endLine = Math.min(document.getLineCount() - 1, currentLine + BOTTOM_CONTEXT_LINES);

        StringBuilder lines = new StringBuilder();
        for (int i = startLine; i <= endLine; i++) {
            int lineStartOffset = document.getLineStartOffset(i);
            int lineEndOffset = document.getLineEndOffset(i);
            String lineText = document.getText(new TextRange(lineStartOffset, lineEndOffset));

            if (i == currentLine) {
                int caretPositionInLine = currentOffset - lineStartOffset;
                lineText = lineText.substring(0, caretPositionInLine) + "{caret is here}"
                        + lineText.substring(caretPositionInLine);
            }

            lines.append(lineText).append("\n");
        }
        return lines.toString();
    }

    /**
     * Debounce a given task so that rapid key events cancel previous tasks.
     */
    private void debounce(Runnable task, int delayMs) {
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        pendingTask = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        if (pendingTask != null) {
            pendingTask.cancel(true);
        }

        scheduler.shutdownNow();

        // Clean up actions
        if (currentTabAction != null) {
            currentTabAction = null;
        }
        if (currentEscapeAction != null) {
            currentEscapeAction = null;
        }
    }
}