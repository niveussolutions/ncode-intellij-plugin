package com.technology.ncode.GoogleThis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;

@ExtendWith(MockitoExtension.class)
class GoogleThisTest {

    private GoogleThis googleThis;
    private AnActionEvent mockEvent;
    private Editor mockEditor;
    private CaretModel mockCaretModel;
    private Caret mockCaret;
    private Presentation mockPresentation;

    @BeforeEach
    void setUp() {
        googleThis = new GoogleThis();
        mockEvent = mock(AnActionEvent.class);
        mockEditor = mock(Editor.class);
        mockCaretModel = mock(CaretModel.class);
        mockCaret = mock(Caret.class);
        mockPresentation = mock(Presentation.class);

        // Setup only necessary mocking behavior
        when(mockEvent.getRequiredData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
        when(mockEditor.getCaretModel()).thenReturn(mockCaretModel);
        when(mockCaretModel.getCurrentCaret()).thenReturn(mockCaret);
        when(mockEvent.getPresentation()).thenReturn(mockPresentation);
    }

    @Test
    void testUpdate_withSelection() {
        // Setup
        when(mockCaret.hasSelection()).thenReturn(true);

        // Invoke
        googleThis.update(mockEvent);

        // Verify
        verify(mockPresentation).setEnabledAndVisible(true);
    }

    @Test
    void testUpdate_withoutSelection() {
        // Setup
        when(mockCaret.hasSelection()).thenReturn(false);

        // Invoke
        googleThis.update(mockEvent);

        // Verify
        verify(mockPresentation).setEnabledAndVisible(false);
    }
}
