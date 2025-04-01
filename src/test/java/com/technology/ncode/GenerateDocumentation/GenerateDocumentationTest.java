package com.technology.ncode.GenerateDocumentation;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.mockito.Mockito.*;

class GenerateDocumentationTest {

    @Test
    void testActionPerformed_withValidSelection() {
        // Arrange
        GenerateDocumentation action = new GenerateDocumentation();
        AnActionEvent mockEvent = mock(AnActionEvent.class);
        Project mockProject = mock(Project.class);
        Editor mockEditor = mock(Editor.class);
        SelectionModel mockSelectionModel = mock(SelectionModel.class);
        ToolWindowManager mockToolWindowManager = mock(ToolWindowManager.class);
        ToolWindow mockToolWindow = mock(ToolWindow.class);
        JComponent mockComponent = mock(JComponent.class);

        // Mock the IntelliJ Platform services
        mockStatic(ToolWindowManager.class);
        when(ToolWindowManager.getInstance(mockProject)).thenReturn(mockToolWindowManager);
        when(mockToolWindowManager.getToolWindow("GenerateDocumentationToolWindow")).thenReturn(mockToolWindow);

        // Mock the tool window component
        when(mockToolWindow.getComponent()).thenReturn(mockComponent);

        // Mock the editor and selection
        when(mockEvent.getProject()).thenReturn(mockProject);
        when(mockEvent.getData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
        when(mockEditor.getSelectionModel()).thenReturn(mockSelectionModel);
        when(mockSelectionModel.getSelectedText()).thenReturn("Selected code");

        // Act
        action.actionPerformed(mockEvent);

        // Assert
        verify(mockToolWindow).activate(null);
        verify(mockComponent).removeAll();
        verify(mockComponent).add(any(JPanel.class));
    }

    @Test
    void testActionPerformed_withNoSelection() {
        // Arrange
        GenerateDocumentation action = new GenerateDocumentation();
        AnActionEvent mockEvent = mock(AnActionEvent.class);
        Project mockProject = mock(Project.class);
        Editor mockEditor = mock(Editor.class);
        SelectionModel mockSelectionModel = mock(SelectionModel.class);

        when(mockEvent.getProject()).thenReturn(mockProject);
        when(mockEvent.getData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
        when(mockEditor.getSelectionModel()).thenReturn(mockSelectionModel);
        when(mockSelectionModel.getSelectedText()).thenReturn(null);

        // Act
        action.actionPerformed(mockEvent);

        // Assert
        verify(mockSelectionModel).getSelectedText();
    }

    @Test
    void testActionPerformed_withEmptySelection() {
        // Arrange
        GenerateDocumentation action = new GenerateDocumentation();
        AnActionEvent mockEvent = mock(AnActionEvent.class);
        Project mockProject = mock(Project.class);
        Editor mockEditor = mock(Editor.class);
        SelectionModel mockSelectionModel = mock(SelectionModel.class);

        when(mockEvent.getProject()).thenReturn(mockProject);
        when(mockEvent.getData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
        when(mockEditor.getSelectionModel()).thenReturn(mockSelectionModel);
        when(mockSelectionModel.getSelectedText()).thenReturn("");

        // Act
        action.actionPerformed(mockEvent);

        // Assert
        verify(mockSelectionModel).getSelectedText();
    }

    @Test
    void testActionPerformed_withNullProject() {
        // Arrange
        GenerateDocumentation action = new GenerateDocumentation();
        AnActionEvent mockEvent = mock(AnActionEvent.class);

        when(mockEvent.getProject()).thenReturn(null);

        // Act
        action.actionPerformed(mockEvent);

        // Assert
        verify(mockEvent, never()).getData(CommonDataKeys.EDITOR);
    }

    @Test
    void testActionPerformed_withNullEditor() {
        // Arrange
        GenerateDocumentation action = new GenerateDocumentation();
        AnActionEvent mockEvent = mock(AnActionEvent.class);
        Project mockProject = mock(Project.class);

        when(mockEvent.getProject()).thenReturn(mockProject);
        when(mockEvent.getData(CommonDataKeys.EDITOR)).thenReturn(null);

        // Act
        action.actionPerformed(mockEvent);

        // Assert
        verify(mockProject, never()).getService(ToolWindowManager.class);
    }
}
