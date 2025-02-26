package com.technology.ncode;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import java.util.Timer;
import java.util.TimerTask;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import java.awt.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import java.util.concurrent.atomic.AtomicReference;

public class NCodeInlineCompletionProvider extends TypedHandlerDelegate implements Disposable {
    private final Timer timer = new Timer(true); // Make it a daemon timer
    private final AtomicReference<TimerTask> lastTask = new AtomicReference<>();
    private AnAction currentTabAction;
    private AnAction currentEscapeAction;

    private String generatedTextCache = null;
    private int generatedTextOffset = -1;
    private int highlighterId = -1; // Keep track of highlighter ID

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        debounce(() -> {
            // Schedule modifications on the EDT and wrap them in a write action
            ApplicationManager.getApplication().invokeLater(() -> {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    // Get the current caret offset
                    int offset = editor.getCaretModel().getOffset();
                    // Get the surrounding lines text
                    String surroundingLines = getSurroundingLines(editor);
                    System.out.println("----------");
                    System.out.println("Surrounding lines: " + surroundingLines);
                    System.out.println("----------");
                    // THE VERTEX AI
                    System.out.println("Testing Vertex AI code");
                    InlineVertexAi inlineVertexAi = new InlineVertexAi();
                    try {
                        GenerateContentResponse response = inlineVertexAi.generateContent(surroundingLines);

                        // Extract text response safely
                        String generatedText = InlineVertexAi.extractGeneratedText(response);
                        if (generatedText != null && !generatedText.isEmpty()) {
                            // Store the generated text and offset
                            generatedTextCache = generatedText;
                            generatedTextOffset = offset;

                            // Insert the completion text at the caret
                            editor.getDocument().insertString(offset, generatedText);

                            // Apply transparent highlighting
                            applyTransparentHighlighting(editor, offset, offset + generatedText.length(), editor);

                            // Install custom actions for tab and escape
                            installTabCompletionAction(editor);
                            installEscapeAction(editor);

                        } else {
                            System.out.println("No valid completion generated. Raw response: " + response);
                            clearCache(editor);
                        }

                    } catch (IOException e) {
                        System.err.println("Error calling Vertex AI: " + e.getMessage());
                        e.printStackTrace();
                        clearCache(editor);
                    } catch (Exception e) {
                        System.err.println("Unexpected error: " + e.getMessage());
                        e.printStackTrace();
                        clearCache(editor);
                    }

                    System.out.println("Vertex AI code test complete");
                });
            });
        }, 3000); // 3000ms debounce delay

        return Result.CONTINUE;
    }

    private void clearCache(Editor editor) {
        generatedTextCache = null;
        generatedTextOffset = -1;
        removeHighlighting(editor);
    }

    private void applyTransparentHighlighting(Editor editor, int startOffset, int endOffset, Editor currentEditor) {
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
                    40)); // 40 alpha for subtle background
        } else {
            // Fallback color if selection color is not defined
            attributes.setBackgroundColor(new Color(200, 200, 200, 40));
        }

        // Set a subtle foreground color
        Color defaultForeground = scheme.getDefaultForeground();
        if (defaultForeground != null) {
            attributes.setForegroundColor(new Color(
                    defaultForeground.getRed(),
                    defaultForeground.getGreen(),
                    defaultForeground.getBlue(),
                    180)); // 180 alpha for readable but subtle text
        }

        MarkupModel markupModel = editor.getMarkupModel();
        RangeHighlighter highlighter = markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                attributes,
                HighlighterTargetArea.EXACT_RANGE);
        highlighterId = highlighter.hashCode();
    }

    private void removeHighlighting(Editor editor) {
        if (highlighterId != -1) {
            MarkupModel markupModel = editor.getMarkupModel();
            markupModel.removeAllHighlighters();
            highlighterId = -1;
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
                    // If there's an active suggestion, accept it
                    removeHighlighting(editor);
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
        currentEscapeAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (generatedTextCache != null && generatedTextOffset != -1) {
                    Project project = editor.getProject();
                    if (project != null) {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                Document document = editor.getDocument();
                                if (generatedTextOffset + generatedTextCache.length() <= document.getTextLength()) {
                                    document.deleteString(generatedTextOffset,
                                            generatedTextOffset + generatedTextCache.length());
                                }
                            } catch (Exception ex) {
                                System.err.println("Error removing completion text: " + ex.getMessage());
                            } finally {
                                removeHighlighting(editor);
                                clearCache(editor);
                            }
                        });
                    }
                }
            }
        };
        currentEscapeAction.registerCustomShortcutSet(
                actionManager.getAction(IdeActions.ACTION_EDITOR_ESCAPE).getShortcutSet(),
                editor.getContentComponent());
    }

    private String getSurroundingLines(Editor editor) {
        int TOP_CONTEXT_LINES = 10;
        int BOTTOM_CONTEXT_LINES = 5;

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
    private void debounce(Runnable task, int delay) {
        TimerTask oldTask = lastTask.get();
        if (oldTask != null) {
            oldTask.cancel();
        }

        TimerTask newTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        if (lastTask.compareAndSet(oldTask, newTask)) {
            timer.schedule(newTask, delay);
        } else {
            newTask.cancel();
        }
    }

    @Override
    public void dispose() {
        timer.cancel();
        if (currentTabAction != null) {
            currentTabAction = null;
        }
        if (currentEscapeAction != null) {
            currentEscapeAction = null;
        }
    }
}