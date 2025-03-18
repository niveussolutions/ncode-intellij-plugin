package com.technology.ncode.GenerateDocumentation;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class GenerateDocumentation extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get the current project
        Project project = e.getProject();
        if (project == null)
            return;

        // Get the editor
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (editor == null)
            return;

        // Get selected text
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        // Ensure selectedText is not null
        if (selectedText == null || selectedText.isEmpty()) {
            return;
        }

        // Get or create the tool window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("GenerateDocumentationToolWindow");

        if (toolWindow == null) {
            toolWindow = ToolWindowManager.getInstance(project).registerToolWindow("GenerateDocumentationToolWindow",
                    true, ToolWindowAnchor.RIGHT);
        }

        // Pass the selected text to the tool window content
        GenerateDocumentationFactoryContent content = new GenerateDocumentationFactoryContent();
        content.setSelectedCode(selectedText);

        // Update the tool window with new content
        toolWindow.getComponent().removeAll();
        toolWindow.getComponent().add(content.getPanel());
        toolWindow.activate(null);
    }
}
