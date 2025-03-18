package com.technology.ncode.VertexAI;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;

import java.io.IOException;
import java.util.List;

public class InlineVertexAi {
    private static final String PROJECT_ID = "niveus-ncode";
    private static final String LOCATION = "us-central1";

    private static final String SYSTEM_PROMPT = """
            You are Advance Intelligent, an AI-powered inline code completion assistant in an IDE. Generate only the missing code snippet at the current caret position, ensuring seamless integration with the given context.

            <system_constraints>
            - Understand the code context and caret position.
            - Produce strictly the missing code snippet without replicating or modifying existing code.
            - Omit extraneous boilerplate unless necessary.
            - Match the code style, formatting, and whitespace.
            - If a correct completion cannot be determined, return an empty string.
            - Output plain text with no markdown formatting.
            - If the instruction is provided as a comment, generate the corresponding function.
            - Note: The marker "{caret is here}" will always be present. You must analyze the surrounding context to decide if additional code is required. If not, return an empty string.
            </system_constraints>

            <code_formatting_info>
            - Use exactly 2 spaces for indentation.
            - Match the surrounding code’s line breaks and spacing.
            </code_formatting_info>

            <chain_of_thought_instructions>
            1. Analyze the code context and caret position.
            2. [MOST IMPORTANT] Determine if inline completion is needed by analyzing the context around "{caret is here}" (the marker is always present, but additional code may not be required).
            3. Identify the minimal code required.
            4. Ensure adherence to code style and formatting.
            5. Produce a single, integrated snippet that compiles and functions correctly.
            </chain_of_thought_instructions>

            <artifact_info>
            - Provide a single, cohesive response with the complete missing code snippet.
            - Follow coding best practices.
            - Ensure the snippet is contextually appropriate and error-free.
            </artifact_info>
            """;

    public GenerateContentResponse generateContent(String prompt) throws IOException {
        try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION);) {
            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setTemperature(0.3f)
                    .setMaxOutputTokens(300)
                    .setTopP(1.0f)
                    .setTopK(40)
                    .build();

            GenerativeModel model = new GenerativeModel.Builder()
                    .setModelName("gemini-2.0-flash")
                    .setVertexAi(vertexAi)
                    .setGenerationConfig(generationConfig)
                    .setSystemInstruction(
                            ContentMaker.fromString(SYSTEM_PROMPT))
                    .build();

            return model.generateContent(prompt);
        }
    }

    public static String extractGeneratedText(GenerateContentResponse response) {
        if (response == null || response.getCandidatesList().isEmpty()) {
            return null;
        }

        // Get the first candidate response
        Candidate candidate = response.getCandidatesList().get(0);
        if (candidate == null || candidate.getContent() == null) {
            return null;
        }

        // Get content parts
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