package com.technology.ncode;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
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

public class NCodeInlineCompletionProvider extends TypedHandlerDelegate {
    // Use a Timer instance for debounce
    private Timer timer = new Timer();
    private TimerTask lastTask;

    private String generatedTextCache = null;
    private int generatedTextOffset = -1;

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
                            // Select the inserted text so it's easy to accept/reject
                            editor.getSelectionModel().setSelection(offset, offset + generatedText.length());

                            // Install custom actions for tab and escape
                            installTabCompletionAction(editor);
                            installEscapeAction(editor);

                        } else {
                            System.out.println("No valid completion generated. Raw response: " + response);
                            clearCache();
                        }

                    } catch (IOException e) {
                        System.err.println("Error calling Vertex AI: " + e.getMessage());
                        e.printStackTrace();
                        clearCache();
                    } catch (Exception e) {
                        System.err.println("Unexpected error: " + e.getMessage());
                        e.printStackTrace();
                        clearCache();
                    }

                    System.out.println("Vertex AI code test complete");
                });
            });
        }, 3000); // 3000ms debounce delay

        return Result.CONTINUE;
    }

    private void clearCache() {
        generatedTextCache = null;
        generatedTextOffset = -1;
    }

    private void installTabCompletionAction(Editor editor) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction tabAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (generatedTextCache != null && generatedTextOffset != -1) {
                    // Accept the completion. Nothing needs to be done here, as insertion already
                    // happened. Just clear selection and cache
                    editor.getSelectionModel().removeSelection();
                    clearCache();
                }
            }
        };
        tabAction.registerCustomShortcutSet(
                actionManager.getAction(IdeActions.ACTION_EDITOR_TAB).getShortcutSet(), editor.getContentComponent());
    }

    private void installEscapeAction(Editor editor) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction escapeAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (generatedTextCache != null && generatedTextOffset != -1) {
                    // Remove the inserted text
                    WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                        Document document = editor.getDocument();
                        document.deleteString(generatedTextOffset, generatedTextOffset + generatedTextCache.length());
                        clearCache();
                    });
                }
            }
        };
        escapeAction.registerCustomShortcutSet(
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
        // Cancel the previous scheduled task if it exists
        if (lastTask != null) {
            lastTask.cancel();
        }
        // Create a new TimerTask with the provided task
        lastTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };
        timer.schedule(lastTask, delay);
    }
}