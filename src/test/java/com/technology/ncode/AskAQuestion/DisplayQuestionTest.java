package com.technology.ncode.AskAQuestion;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        when(mockContent.getComponent()).thenReturn(new DisplayQuestionToolWindowContent());
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

        displayQuestionAction.actionPerformed(mockEvent);

        // Verify toolWindowManager.getToolWindow is called only once
        verify(mockToolWindowManager, times(1)).getToolWindow("DisplayQuestionToolWindow");
        verify(mockToolWindow, times(1)).activate(null);
    }

    @Test
    void testSetSelectedCode() throws Exception {
        DisplayQuestionToolWindowContent content = new DisplayQuestionToolWindowContent();
        content.setSelectedCode("Sample Code");

        // Wait for the SwingUtilities.invokeLater() to complete
        SwingUtilities.invokeAndWait(() -> {
        });

        assertEquals("Sample Code", content.getSelectedCode());
    }
}
