package com.technology.ncode.VertexAI;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentationVertexAiTest {

    @Test
    void testGenerateContentStream_validPrompt() throws IOException {
        // Arrange
        DocumentationVertexAi documentationVertexAi = new DocumentationVertexAi();
        String testPrompt = "Test prompt for content stream generation";

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
                            ResponseStream<GenerateContentResponse> mockResponseStream = mock(ResponseStream.class);
                            Iterator<GenerateContentResponse> mockIterator = mock(Iterator.class);
                            when(mockIterator.hasNext()).thenReturn(true, false);
                            when(mockIterator.next()).thenReturn(mockResponse);
                            when(mockResponseStream.iterator()).thenReturn(mockIterator);
                            when(mock.generateContentStream(testPrompt)).thenReturn(mockResponseStream);
                        })) {

            // Act
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            Consumer<String> onNext = response -> {
                assertEquals("Generated documentation text", response);
                callbackCalled.set(true);
            };

            documentationVertexAi.generateContentStream(testPrompt, onNext);

            // Assert
            assertTrue(callbackCalled.get(), "Callback was not called");
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContentStream_nullPrompt() {
        // Arrange
        DocumentationVertexAi documentationVertexAi = new DocumentationVertexAi();
        Consumer<String> onNext = mock(Consumer.class);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> documentationVertexAi.generateContentStream(null, onNext));
    }

    @Test
    void testGenerateContentStream_emptyPrompt() {
        // Arrange
        DocumentationVertexAi documentationVertexAi = new DocumentationVertexAi();
        Consumer<String> onNext = mock(Consumer.class);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> documentationVertexAi.generateContentStream("   ", onNext));
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