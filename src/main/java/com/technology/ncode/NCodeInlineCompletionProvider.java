package com.technology.ncode;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
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

                    // TEST THE VERTEX AI CODE HERE
                    System.out.println("Testing Vertex AI code");
                    InlineVertexAi inlineVertexAi = new InlineVertexAi();
                    try {
                        String prompt = surroundingLines;
                        GenerateContentResponse response = inlineVertexAi.generateContent(prompt);

                        // Extract text response safely
                        String generatedText = InlineVertexAi.extractGeneratedText(response);

                        if (generatedText != null && !generatedText.isEmpty()) {
                            System.out.println("Generated Text:\n" + generatedText);
                            // Insert the completion text at the caret
                            editor.getDocument().insertString(offset, generatedText);
                            // Select the inserted text so it's easy to accept/reject
                            editor.getSelectionModel().setSelection(offset, offset + generatedText.length());
                        } else {
                            System.out.println("No valid completion generated. Raw response: " + response);
                        }

                    } catch (IOException e) {
                        System.err.println("Error calling Vertex AI: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.err.println("Unexpected error: " + e.getMessage());
                        e.printStackTrace();
                    }

                    System.out.println("Vertex AI code test complete");
                });
            });
        }, 3000); // 3000ms debounce delay

        return Result.CONTINUE;
    }

    private String getSurroundingLines(Editor editor) {
        int TOP_CONTEXT_LINES = 10;
        int BOTTOM_CONTEXT_LINES = 5;

        CaretModel caretModel = editor.getCaretModel();
        int currentLine = caretModel.getLogicalPosition().line;
        Document document = editor.getDocument();
        int startLine = Math.max(0, currentLine - TOP_CONTEXT_LINES);
        int endLine = Math.min(document.getLineCount() - 1, currentLine + BOTTOM_CONTEXT_LINES);

        // print top start lines
        System.out.println("Start Line: " + startLine);

        StringBuilder topLines = new StringBuilder();
        for (int i = startLine; i <= currentLine; i++) {
            topLines.append(
                    document.getText(new TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i))));
            topLines.append("\n");
        }

        System.out.println("Bottom LInes:\n" + topLines.toString());
        StringBuilder bottonLines = new StringBuilder();
        for (int i = currentLine; i <= endLine; i++) {
            bottonLines.append(
                    document.getText(new TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i))));
            bottonLines.append("\n");
        }
        System.out.println("bottonLines :\n" + bottonLines.toString());

        StringBuilder lines = new StringBuilder();
        for (int i = startLine; i <= endLine; i++) {
            lines.append(
                    document.getText(new TextRange(document.getLineStartOffset(i),
                            document.getLineEndOffset(i))));
            lines.append("\n");
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
