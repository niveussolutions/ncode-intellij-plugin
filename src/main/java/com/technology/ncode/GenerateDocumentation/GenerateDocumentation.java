package com.technology.ncode.GenerateDocumentation;

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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class GenerateDocumentation extends AnAction implements DumbAware {

    // Persist content instance to reuse panel and preserve chat history
    private static GenerateDocumentationFactoryContent content;

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
        ToolWindow toolWindow = toolWindowManager.getToolWindow("ncode-DocumentGeneration");

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("ncode-DocumentGeneration", true, ToolWindowAnchor.RIGHT);
        }

        if (content == null) {
            content = new GenerateDocumentationFactoryContent();
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content toolWindowContent = contentFactory.createContent(content.getPanel(), "", false);
            toolWindow.getContentManager().addContent(toolWindowContent);

            // Add Clear Chat button
            AnAction clearChatAction = new AnAction("Clear Chat", "Clear the current chat history",
                    com.intellij.icons.AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    content.clearChatHistory();
                }
            };
            toolWindow.setTitleActions(Collections.singletonList(clearChatAction));
        }

        // Dynamically update the selected code in the same window
        content.setSelectedCode(selectedText != null ? selectedText : "");

        // Activate and bring to front
        toolWindow.activate(null);
    }
}