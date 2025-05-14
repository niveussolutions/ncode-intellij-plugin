package com.technology.ncode.VertexAI;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

class DocumentationVertexAiTest {

    @Test
    void testGenerateContent_validPrompt() throws IOException {
        // Arrange
        DocumentationVertexAi documentationVertexAi = new DocumentationVertexAi();
        String testPrompt = "Test prompt for content generation";

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(true);
        when(mockPart.getText()).thenReturn("Generated documentation text");

        try (MockedConstruction<VertexAI> vertexAiMock = Mockito.mockConstruction(VertexAI.class);
                MockedConstruction<GenerativeModel> modelMock = Mockito.mockConstruction(GenerativeModel.class,
                        (mock, context) -> {
                            when(mock.generateContent(testPrompt)).thenReturn(mockResponse);
                        })) {

            // Act
            GenerateContentResponse response = documentationVertexAi.generateContent(testPrompt);
            String result = DocumentationVertexAi.extractGeneratedDocumentation(response);

            // Assert
            assertEquals("Generated documentation text", result);
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContent_nullPrompt() {
        // Arrange
        DocumentationVertexAi documentationVertexAi = new DocumentationVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> documentationVertexAi.generateContent(null));
    }

    @Test
    void testGenerateContent_emptyPrompt() {
        // Arrange
        DocumentationVertexAi documentationVertexAi = new DocumentationVertexAi();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> documentationVertexAi.generateContent("   "));
    }

    @Test
    void testExtractGeneratedDocumentation_validResponse() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(true);
        when(mockPart.getText()).thenReturn("Generated documentation text");

        // Act
        String result = DocumentationVertexAi.extractGeneratedDocumentation(mockResponse);

        // Assert
        assertEquals("Generated documentation text", result);
    }

    @Test
    void testExtractGeneratedDocumentation_nullResponse() {
        // Act
        String result = DocumentationVertexAi.extractGeneratedDocumentation(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractGeneratedDocumentation_emptyResponse() {
        // Arrange
        GenerateContentResponse emptyResponse = mock(GenerateContentResponse.class);
        when(emptyResponse.getCandidatesList()).thenReturn(Collections.emptyList());

        // Act
        String result = DocumentationVertexAi.extractGeneratedDocumentation(emptyResponse);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractGeneratedDocumentation_noTextParts() {
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
        String result = DocumentationVertexAi.extractGeneratedDocumentation(mockResponse);

        // Assert
        assertNull(result);
    }
}