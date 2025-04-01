package com.technology.ncode.InlineCodeCompletion;

import com.google.api.core.ApiFuture;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.technology.ncode.VertexAI.InlineVertexAi;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provides inline code completion functionality using Vertex AI.
 * Monitors typing and displays AI-generated code suggestions after a debounce
 * period.
 */
public class NCodeInlineCompletionProvider extends TypedHandlerDelegate implements Disposable {
    private static final Logger LOG = Logger.getInstance(NCodeInlineCompletionProvider.class);

    // Configuration constants
    private static final int DEBOUNCE_DELAY_MS = 2000;
    private static final int TOP_CONTEXT_LINES = 10;
    private static final int BOTTOM_CONTEXT_LINES = 5;
    private static final int HIGHLIGHTING_ALPHA = 40;
    private static final int FOREGROUND_ALPHA = 180;
    private static final int FALLBACK_BG_RED = 200;
    private static final int FALLBACK_BG_GREEN = 200;
    private static final int FALLBACK_BG_BLUE = 200;
    private static final int COOLDOWN_AFTER_ACCEPT_MS = 1000;

    // Use IntelliJ's executor service for better integration with the platform
    private final ScheduledExecutorService scheduler = AppExecutorUtil.getAppScheduledExecutorService();
    private final AtomicReference<ScheduledFuture<?>> pendingTask = new AtomicReference<>();

    // Completion state
    private CompletionState completionState;

    // Key listener to handle all key presses
    private KeyListener activeKeyListener;

    // Document listener to handle document changes
    private DocumentListener documentListener;

    // Flag to prevent suggestion after accepting one
    private boolean suggestionJustAccepted = false;

    // Timer to reset the suggestion-just-accepted flag
    private ScheduledFuture<?> suggestionCooldownTask;

    // Flag to track if metrics have been reported for the current suggestion
    private AtomicBoolean metricsReported = new AtomicBoolean(false);

    // We're using static inner classes to improve memory efficiency
    public static class CompletionState {
        final String text;
        final int offset;
        final RangeHighlighter highlighter;
        final AnAction tabAction;

        CompletionState(String text, int offset, RangeHighlighter highlighter, AnAction tabAction) {
            this.text = text;
            this.offset = offset;
            this.highlighter = highlighter;
            this.tabAction = tabAction;
        }

        void cleanup(Editor editor) {
            if (highlighter != null && highlighter.isValid()) {
                editor.getMarkupModel().removeHighlighter(highlighter);
            }

            if (tabAction != null) {
                tabAction.unregisterCustomShortcutSet(editor.getContentComponent());
            }
        }
    }

    public NCodeInlineCompletionProvider() {
        // Register to get notifications for all editors
        com.intellij.openapi.editor.EditorFactory.getInstance().addEditorFactoryListener(
                new EditorFactoryAdapter() {
                    @Override
                    public void editorCreated(@NotNull EditorFactoryEvent event) {
                        setupEditorListeners(event.getEditor());
                    }
                },
                this // Disposable to ensure listener is removed when provider is disposed
        );
    }

    public void setupEditorListeners(Editor editor) {
        // Create and install document listener
        documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                // If there's an active completion, any document change should cancel it
                if (completionState != null) {
                    cleanupCurrentCompletion(editor, false);
                }

                // Don't trigger completion if a suggestion was just accepted
                if (suggestionJustAccepted) {
                    return;
                }

                // Trigger completion after debounce period for all document changes
                Project project = editor.getProject();
                if (project != null && !project.isDisposed()) {
                    debounce(() -> processCompletion(project, editor), DEBOUNCE_DELAY_MS);
                }
            }
        };

        // Add the document listener to the editor's document
        editor.getDocument().addDocumentListener(documentListener);
    }

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        // If there's an active completion, any character typed should cancel it
        if (completionState != null) {
            cleanupCurrentCompletion(editor, false);
        }

        // Character typing is already handled by the document listener
        return Result.CONTINUE;
    }

    public void processCompletion(Project project, Editor editor) {
        // Avoid processing if editor is disposed or project is closed
        if (project.isDisposed() || editor.isDisposed()) {
            return;
        }

        // Skip if we're in the cooldown period after accepting a suggestion
        if (suggestionJustAccepted) {
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
                    cleanupCurrentCompletion(editor, false);

                    // Get the current caret offset
                    int offset = editor.getCaretModel().getOffset();

                    // Get the surrounding lines text
                    String surroundingLines = getSurroundingLines(editor);
                    LOG.debug("Processing completion with surrounding lines: " + surroundingLines);

                    generateCompletion(editor, offset, surroundingLines);
                } catch (Exception e) {
                    LOG.error("Error processing completion", e);
                    cleanupCurrentCompletion(editor, false);
                }
            });
        });
    }

    public void generateCompletion(Editor editor, int offset, String surroundingLines) {
        // Skip if we're already showing a completion
        if (completionState != null) {
            return;
        }

        InlineVertexAi inlineVertexAi = new InlineVertexAi();
        try {
            ApiFuture<GenerateContentResponse> future = inlineVertexAi.generateContentAsync(surroundingLines);

            // Handle the future response asynchronously
            ListenableFuture<GenerateContentResponse> listenableFuture = Futures.immediateFuture(future.get());
            Futures.addCallback(listenableFuture, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse response) {
                    String generatedText = InlineVertexAi.extractGeneratedText(response);

                    if (generatedText != null && !generatedText.isEmpty()) {
                        // Schedule modifications on the EDT and wrap them in a write action
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (editor.isDisposed()) {
                                return;
                            }
                            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                                try {
                                    // Insert the completion text at the caret
                                    Document document = editor.getDocument();

                                    // Temporarily remove document listener to avoid recursive triggering
                                    if (documentListener != null) {
                                        document.removeDocumentListener(documentListener);
                                    }

                                    document.insertString(offset, generatedText);

                                    // Re-add document listener
                                    if (documentListener != null) {
                                        document.addDocumentListener(documentListener);
                                    }

                                    // Reset the metrics reported flag for this new suggestion
                                    metricsReported.set(false);

                                    // Apply transparent highlighting
                                    RangeHighlighter highlighter = applyTransparentHighlighting(editor, offset,
                                            offset + generatedText.length());

                                    // Install custom action for tab
                                    AnAction tabAction = installTabCompletionAction(editor, offset, generatedText);

                                    // Install the key listener for handling other keys
                                    installKeyListener(editor, offset, generatedText);

                                    // Store the completion state
                                    completionState = new CompletionState(
                                            generatedText,
                                            offset,
                                            highlighter,
                                            tabAction);
                                } catch (Exception e) {
                                    LOG.error("Error processing completion after async retrieval", e);
                                    cleanupCurrentCompletion(editor, false);
                                }
                            });
                        });
                    } else {
                        LOG.warn("No valid completion generated. Raw response: " + response);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOG.warn("Error calling Vertex AI asynchronously", t);
                }
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            LOG.error("Unexpected error during completion generation", e);
        }
    }

    public void cleanupCurrentCompletion(Editor editor, boolean wasAccepted) {
        // Report metrics for current suggestion if they haven't been reported yet
        if (completionState != null && !metricsReported.getAndSet(true)) {
            UsageMetricsReporter.reportEditorMetrics(editor, completionState.text, wasAccepted);
        }

        Optional.ofNullable(completionState).ifPresent(state -> {
            state.cleanup(editor);
            completionState = null;
        });

        // Remove key listener if it exists
        if (activeKeyListener != null && editor != null) {
            editor.getContentComponent().removeKeyListener(activeKeyListener);
            activeKeyListener = null;
        }
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

                // Set the flag to prevent immediate suggestion after acceptance
                setSuggestionJustAccepted(true);

                // Move the caret to the end of the generated text
                editor.getCaretModel().moveToOffset(endPosition);

                // Clean up and report metrics for an accepted suggestion
                cleanupCurrentCompletion(editor, true);
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

    private void installKeyListener(Editor editor, int offset, String generatedText) {
        // Remove any existing listener
        if (activeKeyListener != null) {
            editor.getContentComponent().removeKeyListener(activeKeyListener);
        }

        // Create a new key listener that will handle all keys except tab
        activeKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // If Tab key is pressed, the tab action will handle it
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    return;
                }

                // For any other key, cancel the completion
                Project project = editor.getProject();
                if (project == null || project.isDisposed()) {
                    return;
                }

                // We need to consume the event to prevent it from being processed twice
                e.consume();

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        Document document = editor.getDocument();

                        // Temporarily remove document listener to avoid recursive triggering
                        if (documentListener != null) {
                            document.removeDocumentListener(documentListener);
                        }

                        int endOffset = offset + generatedText.length();
                        if (endOffset <= document.getTextLength()) {
                            document.deleteString(offset, endOffset);
                        }

                        // Re-add document listener
                        if (documentListener != null) {
                            document.addDocumentListener(documentListener);
                        }
                    } catch (Exception ex) {
                        LOG.error("Error removing completion text", ex);
                    } finally {
                        // Mark suggestion as rejected
                        cleanupCurrentCompletion(editor, false);

                        // After clearing the suggestion, we should re-dispatch the key event
                        // to properly handle the key press
                        KeyEvent newEvent = new KeyEvent(
                                e.getComponent(),
                                e.getID(),
                                e.getWhen(),
                                e.getModifiers(),
                                e.getKeyCode(),
                                e.getKeyChar(),
                                e.getKeyLocation());

                        SwingUtilities.processKeyBindings(newEvent);
                    }
                });
            }
        };

        // Add the key listener to the editor component
        editor.getContentComponent().addKeyListener(activeKeyListener);
    }

    /**
     * Sets the suggestionJustAccepted flag and schedules its reset after the
     * cooldown period.
     */
    private void setSuggestionJustAccepted(boolean value) {
        suggestionJustAccepted = value;

        // Cancel any existing cooldown task
        if (suggestionCooldownTask != null) {
            suggestionCooldownTask.cancel(false);
            suggestionCooldownTask = null;
        }

        // If we're setting the flag to true, schedule a reset
        if (value) {
            suggestionCooldownTask = scheduler.schedule(() -> {
                suggestionJustAccepted = false;
                suggestionCooldownTask = null;
            }, COOLDOWN_AFTER_ACCEPT_MS, TimeUnit.MILLISECONDS);
        }
    }

    public String getSurroundingLines(Editor editor) {
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
    public void debounce(Runnable task, int delayMs) {
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

        // Cancel the suggestion cooldown timer if active
        if (suggestionCooldownTask != null) {
            suggestionCooldownTask.cancel(true);
            suggestionCooldownTask = null;
        }

        // No need to shutdown the scheduler as it's provided by the platform

        // Ensure we clean up any UI components
        completionState = null;
    }

    public CompletionState getCompletionState() {
        return completionState;
    }
}