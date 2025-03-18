package com.technology.ncode.AskAQuestion;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class DisplayQuestionAction extends AnAction {

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

        // Get the ToolWindowManager
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("DisplayQuestionToolWindow");

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("DisplayQuestionToolWindow", true,
                    ToolWindowAnchor.RIGHT);
        } else {
            toolWindowManager.getToolWindow("DisplayQuestionToolWindow").setAnchor(ToolWindowAnchor.RIGHT, null);
        }

        // Ensure content is added properly
        if (toolWindow.getContentManager().getContentCount() == 0) {
            DisplayQuestionToolWindowContent contentPanel = new DisplayQuestionToolWindowContent();
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(contentPanel.getPanel(), "", false);
            toolWindow.getContentManager().addContent(content);
        }

        // Update content with selected code
        Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof DisplayQuestionToolWindowContent) {
            DisplayQuestionToolWindowContent contentPanel = (DisplayQuestionToolWindowContent) content.getComponent();
            contentPanel.setSelectedCode(selectedText);
        }

        // Activate the tool window
        toolWindow.activate(null);
    }

}
