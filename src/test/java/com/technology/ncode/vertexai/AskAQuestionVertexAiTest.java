package com.technology.ncode.vertexai;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

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
    void testGenerateContent_validPrompt() throws IOException {
        // Arrange
        AskAQuestionVertexAi askAQuestionVertexAi = new AskAQuestionVertexAi();
        String testPrompt = "Test prompt for generation";
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);

        // Use MockedConstruction to mock the VertexAI and GenerativeModel
        try (MockedConstruction<VertexAI> vertexAiMock = Mockito.mockConstruction(VertexAI.class);
                MockedConstruction<GenerativeModel> modelMock = Mockito.mockConstruction(GenerativeModel.class,
                        (mock, context) -> {
                            when(mock.generateContent(anyString())).thenReturn(mockResponse);
                        })) {

            // Act
            GenerateContentResponse response = askAQuestionVertexAi.generateContent(testPrompt);

            // Assert
            assertEquals(mockResponse, response);
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContent_nullPrompt() {
        // Arrange
        AskAQuestionVertexAi askAQuestionVertexAi = new AskAQuestionVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> askAQuestionVertexAi.generateContent(null));
    }

    @Test
    void testGenerateContent_emptyPrompt() {
        // Arrange
        AskAQuestionVertexAi askAQuestionVertexAi = new AskAQuestionVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> askAQuestionVertexAi.generateContent(""));
    }
}