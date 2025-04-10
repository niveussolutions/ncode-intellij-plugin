package com.technology.ncode.AskAQuestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;

class DisplayQuestionTest {

    private DisplayQuestionAction displayQuestionAction;
    private Project mockProject;
    private Editor mockEditor;
    private SelectionModel mockSelectionModel;
    private ToolWindowManager mockToolWindowManager;
    private ToolWindow mockToolWindow;
    private ContentManager mockContentManager;
    private Content mockContent;
    private AnActionEvent mockEvent;

    @BeforeEach
    void setUp() {
        displayQuestionAction = new DisplayQuestionAction();
        mockProject = mock(Project.class);
        mockEditor = mock(Editor.class);
        mockSelectionModel = mock(SelectionModel.class);
        mockToolWindowManager = mock(ToolWindowManager.class);
        mockToolWindow = mock(ToolWindow.class);
        mockContentManager = mock(ContentManager.class);
        mockContent = mock(Content.class);
        mockEvent = mock(AnActionEvent.class);

        when(mockEditor.getSelectionModel()).thenReturn(mockSelectionModel);
        when(mockProject.getService(ToolWindowManager.class)).thenReturn(mockToolWindowManager);
        when(mockToolWindowManager.getToolWindow(anyString())).thenReturn(mockToolWindow);
        when(mockToolWindow.getContentManager()).thenReturn(mockContentManager);
        when(mockContentManager.getContentCount()).thenReturn(1);
        when(mockContentManager.getContent(0)).thenReturn(mockContent);
        when(mockEvent.getProject()).thenReturn(mockProject);
        when(mockEvent.getData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
    }

    @Test
    void testActionPerformedWithNoProject() {
        when(mockEvent.getProject()).thenReturn(null);
        displayQuestionAction.actionPerformed(mockEvent);
        verifyNoInteractions(mockEditor, mockToolWindowManager);
    }

    @Test
    void testActionPerformedWithNoEditor() {
        when(mockEvent.getData(CommonDataKeys.EDITOR)).thenReturn(null);
        displayQuestionAction.actionPerformed(mockEvent);
        verifyNoInteractions(mockSelectionModel, mockToolWindowManager);
    }

    @Test
    void testActionPerformedWithNoSelection() {
        when(mockSelectionModel.getSelectedText()).thenReturn(null);
        displayQuestionAction.actionPerformed(mockEvent);
        verifyNoInteractions(mockToolWindowManager);
    }

    @Test
    void testActionPerformedWithValidSelection() {
        when(mockSelectionModel.getSelectedText()).thenReturn("Sample Code");
        when(mockToolWindow.getContentManager()).thenReturn(mockContentManager);

        displayQuestionAction.actionPerformed(mockEvent);

        verify(mockToolWindowManager).getToolWindow("DisplayQuestionToolWindow");
        verify(mockToolWindow).activate(null);
    }

    @Test
    void testToolWindowRegistrationWhenNotExists() {
        when(mockSelectionModel.getSelectedText()).thenReturn("Sample Code");
        when(mockToolWindowManager.getToolWindow("DisplayQuestionToolWindow")).thenReturn(null);
        when(mockToolWindowManager.registerToolWindow(eq("DisplayQuestionToolWindow"), eq(true),
                eq(ToolWindowAnchor.RIGHT)))
                .thenReturn(mockToolWindow);

        displayQuestionAction.actionPerformed(mockEvent);

        verify(mockToolWindowManager).registerToolWindow("DisplayQuestionToolWindow", true, ToolWindowAnchor.RIGHT);
    }

    @Test
    void testContentCreationWhenNoContentExists() {
        ContentFactory mockContentFactory = mock(ContentFactory.class);
        try (MockedStatic<ContentFactory> mockedStatic = mockStatic(ContentFactory.class)) {
            mockedStatic.when(ContentFactory::getInstance).thenReturn(mockContentFactory);
            when(mockSelectionModel.getSelectedText()).thenReturn("Sample Code");
            when(mockContentManager.getContentCount()).thenReturn(0);
            when(mockContentFactory.createContent(any(), anyString(), anyBoolean())).thenReturn(mockContent);

            displayQuestionAction.actionPerformed(mockEvent);

            verify(mockContentManager).addContent(mockContent);
        }
    }

    @Test
    void testToolWindowAnchorSetting() {
        when(mockSelectionModel.getSelectedText()).thenReturn("Sample Code");
        when(mockToolWindow.getContentManager()).thenReturn(mockContentManager);

        displayQuestionAction.actionPerformed(mockEvent);

        verify(mockToolWindow).setAnchor(eq(ToolWindowAnchor.RIGHT), any());
    }
}
