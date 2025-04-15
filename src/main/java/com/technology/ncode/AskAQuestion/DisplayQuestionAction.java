package com.technology.ncode.AskAQuestion;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class DisplayQuestionAction extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        String selectedText = null;
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            selectedText = selectionModel.getSelectedText();
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ncode-AskQuestion");

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("ncode-AskQuestion", true, ToolWindowAnchor.RIGHT);
        }

        DisplayQuestionToolWindowContent newContentPanel = new DisplayQuestionToolWindowContent();

        // If selection exists, send it; otherwise allow user to type manually
        newContentPanel.setSelectedCode(selectedText);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content newContent = contentFactory.createContent(newContentPanel.getPanel(), "", false);

        toolWindow.getContentManager().removeAllContents(true);
        toolWindow.getContentManager().addContent(newContent);

        // Add trash icon to clear chat
        AnAction clearChatAction = new AnAction("Clear Chat", "Clear the current chat history",
                com.intellij.icons.AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                newContentPanel.clearChatHistory();
            }
        };
        toolWindow.setTitleActions(Collections.singletonList(clearChatAction));

        toolWindow.activate(null);
    }
}
