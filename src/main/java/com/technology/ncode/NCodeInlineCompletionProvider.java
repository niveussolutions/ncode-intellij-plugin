package com.technology.ncode;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import java.util.Timer;
import java.util.TimerTask;
import org.jetbrains.annotations.NotNull;

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
                    // Get the current line text up to the caret (limit to 50 characters)
                    String documentText = editor.getDocument().getText();
                    String linePrefix = documentText.substring(Math.max(0, offset - 50), offset);
                    System.out.println("Line prefix: " + linePrefix);

                    // Check if we should show a completion
                    if (shouldShowCompletion(linePrefix)) {
                        System.out.println("Showing completion");
                        // Compute the completion text based on the line prefix
                        String completionText = getCompletionText(linePrefix);
                        System.out.println("Completion text: " + completionText);
                        System.out.println("Offset: " + offset);

                        // Insert the completion text at the caret
                        editor.getDocument().insertString(offset, completionText);
                        // Select the inserted text so it's easy to accept/reject
                        editor.getSelectionModel().setSelection(offset, offset + completionText.length());
                    }
                });
            });
        }, 1000); // 300ms debounce delay

        return Result.CONTINUE;
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

    private boolean shouldShowCompletion(String linePrefix) {
        // Add your trigger conditions here
        return linePrefix.endsWith("System.") ||
                linePrefix.endsWith("test") ||
                linePrefix.endsWith("//") ||
                linePrefix.endsWith("hello");
    }

    private String getCompletionText(String linePrefix) {
        // Add your completion logic here
        if (linePrefix.endsWith("System.")) {
            return "out.println();";
        } else if (linePrefix.endsWith("test")) {
            return " This is a test completion";
        } else if (linePrefix.endsWith("//")) {
            return " TODO: Add implementation";
        } else if (linePrefix.endsWith("hello")) {
            return " Welcome to Niveus!";
        }
        return "";
    }
}
