package com.technology.ncode;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class NCodeInlineCompletionProvider extends TypedHandlerDelegate {
    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        // Get the current line text up to the caret
        int offset = editor.getCaretModel().getOffset();
        String linePrefix = editor.getDocument().getText().substring(Math.max(0, offset - 50), offset);

        // Check if we should show a completion
        if (shouldShowCompletion(linePrefix)) {
            // Insert the completion text
            String completionText = getCompletionText(linePrefix);
            editor.getDocument().insertString(offset, completionText);
            // Make the inserted text selected so it's easy to accept/reject
            editor.getSelectionModel().setSelection(offset, offset + completionText.length());
        }

        return Result.CONTINUE;
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

