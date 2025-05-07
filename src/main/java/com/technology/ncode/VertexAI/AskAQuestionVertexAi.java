package com.technology.ncode.VertexAI;

import java.io.IOException;
import java.util.List;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

public class AskAQuestionVertexAi {
    private static final String PROJECT_ID = "niveus-ncode";
    private static final String LOCATION = "us-central1";

    public GenerateContentResponse generateContent(String prompt) throws IOException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION)) {
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
                    .build();

            return model.generateContent(prompt);
        }
    }

    public static String extractGeneratedText(GenerateContentResponse response) {
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