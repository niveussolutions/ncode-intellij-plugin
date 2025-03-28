package com.technology.ncode.GoogleThis;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

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

        // Setup common mocking behavior
        when(mockEvent.getRequiredData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
        when(mockEditor.getCaretModel()).thenReturn(mockCaretModel);
        when(mockCaretModel.getCurrentCaret()).thenReturn(mockCaret);
        when(mockEvent.getPresentation()).thenReturn(mockPresentation);
    }

    @Test
    void testActionPerformed_withNormalText() {
        try (MockedStatic<BrowserUtil> browserUtilMock = Mockito.mockStatic(BrowserUtil.class)) {
            // Setup
            String testQuery = "Java programming";
            when(mockCaret.getSelectedText()).thenReturn(testQuery);

            // Invoke
            googleThis.actionPerformed(mockEvent);

            // Verify
            String expectedUrl = "https://www.google.com/search?q="
                    + URLEncoder.encode(testQuery, StandardCharsets.UTF_8);
            browserUtilMock.verify(() -> BrowserUtil.browse(expectedUrl), times(1));
        }
    }

    @Test
    void testActionPerformed_withNullText() {
        try (MockedStatic<BrowserUtil> browserUtilMock = Mockito.mockStatic(BrowserUtil.class)) {
            // Setup
            when(mockCaret.getSelectedText()).thenReturn(null);

            // Invoke
            googleThis.actionPerformed(mockEvent);

            // Verify
            browserUtilMock.verifyNoInteractions();
        }
    }

    @Test
    void testActionPerformed_withBlankText() {
        try (MockedStatic<BrowserUtil> browserUtilMock = Mockito.mockStatic(BrowserUtil.class)) {
            // Setup
            when(mockCaret.getSelectedText()).thenReturn("   ");

            // Invoke
            googleThis.actionPerformed(mockEvent);

            // Verify
            browserUtilMock.verifyNoInteractions();
        }
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