package com.technology.ncode.GenerateTestCases;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class GenerateTestCase extends AnAction {

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

        if (selectedText == null || selectedText.isEmpty()) {
            return;
        }

        // Open or create the tool window
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("GenerateTestCaseToolWindow");

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("GenerateTestCaseToolWindow", true,
                    ToolWindowAnchor.RIGHT);
        }

        // Pass the selected text to the panel
        GenerateTestCaseFactoryContent content = new GenerateTestCaseFactoryContent();
        content.setSelectedCode(selectedText);

        // Set the content of the tool window
        toolWindow.getComponent().removeAll();
        toolWindow.getComponent().add(content.getPanel());
        toolWindow.activate(null);
    }
}
