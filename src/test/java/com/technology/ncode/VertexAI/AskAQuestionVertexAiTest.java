package com.technology.ncode.VertexAI;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AskAQuestionVertexAiTest {

    @Test
    void testExtractGeneratedText_validResponse() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        var mockCandidate = mock(com.google.cloud.vertexai.api.Candidate.class);
        var mockContent = mock(com.google.cloud.vertexai.api.Content.class);
        var mockPart = mock(com.google.cloud.vertexai.api.Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(true);
        when(mockPart.getText()).thenReturn("Generated response text");

        // Act
        String result = AskAQuestionVertexAi.extractGeneratedText(mockResponse);

        // Assert
        assertEquals("Generated response text", result);
    }

    @Test
    void testExtractGeneratedText_nullResponse() {
        // Act
        String result = AskAQuestionVertexAi.extractGeneratedText(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractGeneratedText_emptyResponse() {
        // Arrange
        GenerateContentResponse emptyResponse = mock(GenerateContentResponse.class);
        when(emptyResponse.getCandidatesList()).thenReturn(Collections.emptyList());

        // Act
        String result = AskAQuestionVertexAi.extractGeneratedText(emptyResponse);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractGeneratedText_noTextParts() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        var mockCandidate = mock(com.google.cloud.vertexai.api.Candidate.class);
        var mockContent = mock(com.google.cloud.vertexai.api.Content.class);
        var mockPart = mock(com.google.cloud.vertexai.api.Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(false);

        // Act
        String result = AskAQuestionVertexAi.extractGeneratedText(mockResponse);

        // Assert
        assertNull(result);
    }

    @Test
    void testGenerateContentStream_validPrompt() throws IOException {
        // Arrange
        AskAQuestionVertexAi askAQuestionVertexAi = new AskAQuestionVertexAi();
        String testPrompt = "Test prompt for streaming generation";

        // Mock consumer to verify text processing
        Consumer<String> mockConsumer = mock(Consumer.class);

        // Use MockedConstruction to mock the VertexAI and GenerativeModel
        try (MockedConstruction<VertexAI> vertexAiMock = Mockito.mockConstruction(VertexAI.class);
                MockedConstruction<GenerativeModel> modelMock = Mockito.mockConstruction(GenerativeModel.class)) {

            // Act
            assertDoesNotThrow(() -> askAQuestionVertexAi.generateContentStream(testPrompt, mockConsumer));

            // Assert
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContentStream_nullPrompt() {
        // Arrange
        AskAQuestionVertexAi askAQuestionVertexAi = new AskAQuestionVertexAi();

        // Act & Assert
        Consumer<String> mockConsumer = mock(Consumer.class);
        assertThrows(Exception.class, () -> askAQuestionVertexAi.generateContentStream(null, mockConsumer));
    }
}