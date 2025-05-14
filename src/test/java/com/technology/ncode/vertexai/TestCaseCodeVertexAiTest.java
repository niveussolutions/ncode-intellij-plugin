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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestCaseCodeVertexAiTest {

    @Test
    void testGenerateContentStream_validPrompt() throws IOException {
        // Arrange
        TestCaseCodeVertexAi testCaseCodeVertexAi = new TestCaseCodeVertexAi();
        String testPrompt = "Test prompt for test case generation";

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(true);
        when(mockPart.getText()).thenReturn("Generated test case code");

        try (MockedConstruction<VertexAI> vertexAiMock = Mockito.mockConstruction(VertexAI.class);
             MockedConstruction<GenerativeModel> modelMock = Mockito.mockConstruction(GenerativeModel.class,
                     (mock, context) -> {
                         ResponseStream<GenerateContentResponse> mockResponseStream = mock(ResponseStream.class);

                         // Mock the forEach method to actually invoke the consumer
                         // This is the key fix - we need to make the forEach actually process our mockResponse
                         doAnswer(invocation -> {
                             Consumer<GenerateContentResponse> consumer = invocation.getArgument(0);
                             consumer.accept(mockResponse);
                             return null;
                         }).when(mockResponseStream).forEach(any(Consumer.class));

                         when(mock.generateContentStream(testPrompt)).thenReturn(mockResponseStream);
                     })) {

            // Act
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            Consumer<String> onNext = response -> {
                assertEquals("Generated test case code", response);
                callbackCalled.set(true);
            };

            testCaseCodeVertexAi.generateContentStream(testPrompt, onNext);

            // Assert
            assertTrue(callbackCalled.get(), "Callback was not called");
            assertEquals(1, vertexAiMock.constructed().size());
            assertEquals(1, modelMock.constructed().size());
        }
    }

    @Test
    void testGenerateContentStream_nullPrompt() {
        // Arrange
        TestCaseCodeVertexAi testCaseCodeVertexAi = new TestCaseCodeVertexAi();
        Consumer<String> onNext = mock(Consumer.class);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> testCaseCodeVertexAi.generateContentStream(null, onNext));
    }

    @Test
    void testGenerateContentStream_emptyPrompt() {
        // Arrange
        TestCaseCodeVertexAi testCaseCodeVertexAi = new TestCaseCodeVertexAi();
        Consumer<String> onNext = mock(Consumer.class);

        // Act
        try {
            testCaseCodeVertexAi.generateContentStream("   ", onNext);
            // If we reach here, the test should fail as an exception was expected
            fail("Expected exception was not thrown for empty prompt");
        } catch (IOException e) {
            // Expected exception
        }
    }

    @Test
    void testExtractTestCaseCode_validResponse() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(Collections.singletonList(mockPart));
        when(mockPart.hasText()).thenReturn(true);
        when(mockPart.getText()).thenReturn("Generated test case code");

        // Act
        String result = TestCaseCodeVertexAi.extractTestCaseCode(mockResponse);

        // Assert
        assertEquals("Generated test case code", result);
    }

    @Test
    void testExtractTestCaseCode_nullResponse() {
        // Act
        String result = TestCaseCodeVertexAi.extractTestCaseCode(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractTestCaseCode_emptyResponse() {
        // Arrange
        GenerateContentResponse emptyResponse = mock(GenerateContentResponse.class);
        when(emptyResponse.getCandidatesList()).thenReturn(Collections.emptyList());

        // Act
        String result = TestCaseCodeVertexAi.extractTestCaseCode(emptyResponse);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractTestCaseCode_noTextParts() {
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
        String result = TestCaseCodeVertexAi.extractTestCaseCode(mockResponse);

        // Assert
        assertNull(result);
    }

    @Test
    void testExtractTestCaseCode_multipleTextParts() {
        // Arrange
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        Candidate mockCandidate = mock(Candidate.class);
        Content mockContent = mock(Content.class);
        Part mockPart1 = mock(Part.class);
        Part mockPart2 = mock(Part.class);

        when(mockResponse.getCandidatesList()).thenReturn(Collections.singletonList(mockCandidate));
        when(mockCandidate.getContent()).thenReturn(mockContent);
        when(mockContent.getPartsList()).thenReturn(List.of(mockPart1, mockPart2));
        when(mockPart1.hasText()).thenReturn(true);
        when(mockPart1.getText()).thenReturn("First part of ");
        when(mockPart2.hasText()).thenReturn(true);
        when(mockPart2.getText()).thenReturn("test case code");

        // Act
        String result = TestCaseCodeVertexAi.extractTestCaseCode(mockResponse);

        // Assert
        assertEquals("First part of test case code", result);
    }
}