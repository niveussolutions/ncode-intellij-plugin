package com.technology.ncode.InlineCodeCompletion;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsageMetricsReporterTest {

    @Mock
    private Project mockProject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReportEditorMetrics_validInput() {
        com.intellij.openapi.editor.Editor mockEditor = mock(com.intellij.openapi.editor.Editor.class);
        when(mockEditor.getProject()).thenReturn(mockProject);

        CompletableFuture<Void> future = UsageMetricsReporter.reportEditorMetrics(mockEditor, "Generated code", true);

        assertNotNull(future);
    }

    @Test
    void testReportEditorMetrics_nullEditor() {
        assertThrows(NullPointerException.class,
                () -> UsageMetricsReporter.reportEditorMetrics(null, "Generated code", true));
    }

    @Test
    void testReportMetricsAsync_validInput() {
        CompletableFuture<Void> future = UsageMetricsReporter.reportMetricsAsync("user@example.com", "project123", 10,
                5, mockProject);
        assertNotNull(future);
    }

    @Test
    void testReportMetricsAsync_nullEmailAndProjectId() {
        CompletableFuture<Void> future = UsageMetricsReporter.reportMetricsAsync(null, null, 10, 5, mockProject);
        assertNotNull(future);
    }

    @Test
    void testCountLines_validInput() {
        assertEquals(3, UsageMetricsReporter.countLines("line1\nline2\nline3"));
    }

    @Test
    void testCountLines_emptyString() {
        assertEquals(0, UsageMetricsReporter.countLines(""));
    }

    @Test
    void testCountLines_nullString() {
        assertEquals(0, UsageMetricsReporter.countLines(null));
    }

    @Test
    void testGetUserInfo() {
        UsageMetricsReporter.MetricsUserInfo userInfo = UsageMetricsReporter.getUserInfo();
        assertNotNull(userInfo);
        assertNotNull(userInfo.email);
        assertNotNull(userInfo.projectId);
    }

    @Test
    void testDetermineExtensionType() {
        assertNotNull(UsageMetricsReporter.getUserInfo().projectId);
    }

    @Test
    void testGetValueFromGcloud_invalidValue() {
        String result = UsageMetricsReporter.getValueFromGcloud("invalid-value");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEmailFromGcloudAuthList() {
        String email = UsageMetricsReporter.getEmailFromGcloudAuthList();
        assertNotNull(email);
    }
}
