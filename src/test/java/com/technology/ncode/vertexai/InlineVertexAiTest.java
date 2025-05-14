package com.technology.ncode.VertexAI;

import com.google.api.core.ApiFuture;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InlineVertexAiTest {

    @Test
    void testGenerateContentAsync_validPrompt() throws IOException {
        // Arrange
        InlineVertexAi inlineVertexAi = new InlineVertexAi();
        String testPrompt = "Test prompt for async generation";

        // Use MockedConstruction to mock the VertexAI and GenerativeModel
        try (MockedConstruction<VertexAI> vertexAiMock = Mockito.mockConstruction(VertexAI.class);
                MockedConstruction<GenerativeModel> modelMock = Mockito.mockConstruction(GenerativeModel.class,
                        (mock, context) -> {
                            ApiFuture<GenerateContentResponse> mockFuture = mock(ApiFuture.class);
                            when(mock.generateContentAsync(testPrompt)).thenReturn(mockFuture);
                        })) {

            // Act
            ApiFuture<GenerateContentResponse> result = inlineVertexAi.generateContentAsync(testPrompt);

            // Assert
            assertNotNull(result);
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContentAsync_nullPrompt() {
        // Arrange
        InlineVertexAi inlineVertexAi = new InlineVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> inlineVertexAi.generateContentAsync(null));
    }

    @Test
    void testGenerateContentAsync_emptyPrompt() {
        // Arrange
        InlineVertexAi inlineVertexAi = new InlineVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> inlineVertexAi.generateContentAsync("   "));
    }

    @Test
    void testGenerateContent_validPrompt() throws IOException {
        // Arrange
        InlineVertexAi inlineVertexAi = new InlineVertexAi();
        String testPrompt = "Test prompt for content generation";

        // Use MockedConstruction to mock the VertexAI and GenerativeModel
        try (MockedConstruction<VertexAI> vertexAiMock = Mockito.mockConstruction(VertexAI.class);
                MockedConstruction<GenerativeModel> modelMock = Mockito.mockConstruction(GenerativeModel.class,
                        (mock, context) -> {
                            GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
                            when(mock.generateContent(testPrompt)).thenReturn(mockResponse);
                        })) {

            // Act
            GenerateContentResponse result = inlineVertexAi.generateContent(testPrompt);

            // Assert
            assertNotNull(result);
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContent_nullPrompt() {
        // Arrange
        InlineVertexAi inlineVertexAi = new InlineVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> inlineVertexAi.generateContent(null));
    }

    @Test
    void testExtractGeneratedText_validResponse() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(true);
        when(mockPart.getText()).thenReturn("Generated code snippet");

        // Act
        String result = InlineVertexAi.extractGeneratedText(mockResponse);

        // Assert
        assertEquals("Generated code snippet", result);
    }

    @Test
    void testExtractGeneratedText_nullResponse() {
        // Act
        String result = InlineVertexAi.extractGeneratedText(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractGeneratedText_emptyResponse() {
        // Arrange
        GenerateContentResponse emptyResponse = mock(GenerateContentResponse.class);
        when(emptyResponse.getCandidatesList()).thenReturn(Collections.emptyList());

        // Act
        String result = InlineVertexAi.extractGeneratedText(emptyResponse);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractGeneratedText_noTextParts() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(false);

        // Act
        String result = InlineVertexAi.extractGeneratedText(mockResponse);

        // Assert
        assertNull(result);
    }
}