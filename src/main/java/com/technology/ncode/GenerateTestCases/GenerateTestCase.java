package com.technology.ncode.GenerateTestCases;

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

public class GenerateTestCase extends AnAction implements DumbAware {

    // Reuse this to keep content and history in the same tool window
    private static GenerateTestCaseFactoryContent content;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        String selectedText = "";

        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            if (selectionModel.hasSelection()) {
                selectedText = selectionModel.getSelectedText();
            }
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ncode-TestCaseGeneration");

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("ncode-TestCaseGeneration", true, ToolWindowAnchor.RIGHT);
        }

        // First-time setup
        if (content == null) {
            content = new GenerateTestCaseFactoryContent();
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content toolWindowContent = contentFactory.createContent(content.getPanel(), "", false);
            toolWindow.getContentManager().addContent(toolWindowContent);

            // Set up "Clear Chat" button
            AnAction clearChatAction = new AnAction("Clear Chat", "Clear the current chat history",
                    com.intellij.icons.AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    content.clearChatHistory();
                }
            };
            toolWindow.setTitleActions(Collections.singletonList(clearChatAction));
        }

        // Always update selected code
        content.setSelectedCode(selectedText != null ? selectedText : "");

        // Bring focus to the tool window
        toolWindow.activate(null);
    }
}