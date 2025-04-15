package com.technology.ncode.GenerateTestCases;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class GenerateTestCaseToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        GenerateTestCaseFactoryContent content = new GenerateTestCaseFactoryContent();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content toolWindowContent = contentFactory.createContent(content.getPanel(), "", false);
        toolWindow.getContentManager().addContent(toolWindowContent);
    }
}