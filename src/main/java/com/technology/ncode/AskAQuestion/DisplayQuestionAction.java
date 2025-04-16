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

    private static DisplayQuestionToolWindowContent toolWindowContent;

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
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ncode - Chat");

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("ncode - Chat", true, ToolWindowAnchor.RIGHT);
        }

        ContentFactory contentFactory = ContentFactory.getInstance();

        if (toolWindowContent == null) {
            toolWindowContent = new DisplayQuestionToolWindowContent();
            Content content = contentFactory.createContent(toolWindowContent.getPanel(), "", false);
            toolWindow.getContentManager().removeAllContents(true);
            toolWindow.getContentManager().addContent(content);

            AnAction clearChatAction = new AnAction("Clear Chat", "Clear the current chat history",
                    com.intellij.icons.AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    toolWindowContent.clearChatHistory();
                }
            };
            toolWindow.setTitleActions(Collections.singletonList(clearChatAction));
        }

        // Append new selection to previous
        if (selectedText != null && !selectedText.isEmpty()) {
            toolWindowContent.setSelectedCode(selectedText);
        }

        toolWindow.activate(null);
    }
}