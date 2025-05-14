package com.technology.ncode.vertexai;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.technology.ncode.config.EnvironmentConfig;

public class TestCaseCodeVertexAi {
    private static final String PROJECT_ID = EnvironmentConfig.VERTEX_PROJECT_ID;
    private static final String LOCATION = EnvironmentConfig.VERTEX_LOCATION;
    private static final String MODEL_ID = EnvironmentConfig.VERTEX_MODEL_ID;

    private static final String SYSTEM_PROMPT = """
            You are an expert test code generator. Given code, generate executable test code to ensure its correctness.

            Instructions:
            1. Analyze the provided code and understand its functionality.
            2. Generate executable test code that covers various scenarios, including:
               - Normal cases with valid inputs.
               - Edge cases with boundary values.
               - Error cases with invalid inputs.
               - Performance tests, if applicable.
            3. Use appropriate testing frameworks and libraries (e.g., JUnit, TestNG, etc.) based on the language.
            4. For each test case, provide:
               - A clear test method with a descriptive name.
               - Assertions to verify the expected output or behavior.
               - Setup and teardown methods, if necessary.
            5. If the provided code is a class, generate test code for its methods.
            6. If the provided code is a method or function, generate test code for its parameters and return values.
            7. If the code is complex, break it down into smaller parts and generate test code for each part.
            8. If the code involves data structures, generate test code to test their behavior.
            9. Ensure the test code is comprehensive and covers all relevant aspects of the code.
            10. Generate executable code, not just test descriptions.
            11. Ensure that the test code is easy to understand, maintain, and execute.
            12. Adhere to common coding standards and best practices for writing test code.
            13. The generated test code should be able to compile and run without errors.
            """;

    public void generateContentStream(String prompt, Consumer<String> onNext) throws IOException {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }

        if (prompt.trim().isEmpty()) {
            throw new IOException("Prompt cannot be empty or contain only whitespace");
        }

        try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION)) {
            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setTemperature(0.3f)
                    .setMaxOutputTokens(1024)
                    .setTopP(1.0f)
                    .setTopK(40)
                    .build();

            GenerativeModel model = new GenerativeModel.Builder()
                    .setModelName(MODEL_ID)
                    .setVertexAi(vertexAi)
                    .setGenerationConfig(generationConfig)
                    .setSystemInstruction(
                            ContentMaker.fromString(SYSTEM_PROMPT))
                    .build();

            model.generateContentStream(prompt)
                    .forEach(response -> {
                        String text = extractTestCaseCode(response);
                        if (text != null && !text.isEmpty()) {
                            onNext.accept(text);
                        }
                    });
        }
    }

    public String generateContent(String prompt) throws IOException {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }

        if (prompt.trim().isEmpty()) {
            throw new IOException("Prompt cannot be empty or contain only whitespace");
        }

        try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION)) {
            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setTemperature(0.3f)
                    .setMaxOutputTokens(1024)
                    .setTopP(1.0f)
                    .setTopK(40)
                    .build();

            GenerativeModel model = new GenerativeModel.Builder()
                    .setModelName(MODEL_ID)
                    .setVertexAi(vertexAi)
                    .setGenerationConfig(generationConfig)
                    .setSystemInstruction(
                            ContentMaker.fromString(SYSTEM_PROMPT))
                    .build();

            GenerateContentResponse response = model.generateContent(prompt);
            return extractTestCaseCode(response);
        }
    }

    public static String extractTestCaseCode(GenerateContentResponse response) {
        if (response == null || response.getCandidatesList().isEmpty()) {
            return null;
        }

        Candidate candidate = response.getCandidatesList().get(0);
        if (candidate == null || candidate.getContent() == null) {
            return null;
        }

        Content content = candidate.getContent();
        List<Part> parts = content.getPartsList();

        if (parts.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            if (part.hasText()) {
                sb.append(part.getText());
            }
        }

        if (sb.length() == 0) {
            return null;
        }

        return sb.toString();
    }
}