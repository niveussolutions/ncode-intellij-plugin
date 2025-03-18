package com.technology.ncode.VertexAI;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class AskAQuestionVertexAi {
    private static final String PROJECT_ID = "niveus-ncode";
    private static final String LOCATION = "us-central1";

    private static final String SYSTEM_PROMPT = """
            You are an expert coding assistant designed to provide concise and accurate code completions.

            Instructions:
            1. Analyze the surrounding code to understand the context and the user's intent.
            2. Only generate the code that is missing at the current cursor position, marked by "{caret is here}".
            3. Do not generate any code that already exists.
            4. If no code is needed at the cursor position, return an empty string.
            5. Follow the existing code style, including indentation, spacing, and naming conventions.
            6. Return only the code snippet, without any additional explanations or comments, unless the prompt is a comment requiring function generation.
            7. Ensure the generated code is syntactically correct and logically sound.
            8. If a user instruction is provided as a comment, then create the corresponding function.
            """;

    public void generateContentStream(String prompt, Consumer<String> onNext) throws IOException {
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
                    .setSystemInstruction(
                            ContentMaker.fromString(SYSTEM_PROMPT))
                    .build();

            model.generateContentStream(prompt)
                    .forEach(response -> {
                        String text = extractGeneratedText(response);
                        if (text != null && !text.isEmpty()) {
                            onNext.accept(text);
                        }
                    });
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