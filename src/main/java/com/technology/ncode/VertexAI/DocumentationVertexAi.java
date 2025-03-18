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

public class DocumentationVertexAi {
    private static final String PROJECT_ID = "niveus-ncode";
    private static final String LOCATION = "us-central1";

    private static final String SYSTEM_PROMPT = """
            You are an expert documentation generator. Given code, provide comprehensive documentation in Markdown format.

            Instructions:
            1. Analyze the provided code and understand its functionality.
            2. Generate documentation in Markdown format, including:
               - A clear and concise description of the code's purpose.
               - Explanations of classes, methods, and variables.
               - Examples of how to use the code, if applicable.
               - Any relevant notes or considerations.
            3. Ensure the documentation is accurate, complete, and easy to understand.
            4. If the provided code is a class, document the class, its methods and fields.
            5. If the provided code is a method, document the method, its parameters and return values.
            6. If the provided code is a function, document the function, its parameters and return values.
            7. If the provided code is a block of code, document the functionality of that block.
            8. If the code is complex, provide a high-level overview before diving into the details.
            9. If the code is a data structure, explain how it is used and the purpose of its fields.
            10. Make sure to use proper markdown syntax for headings, code blocks, lists, and emphasis.
            11. Do not generate code, only markdown documentation.
            """;

    public void generateContentStream(String prompt, Consumer<String> onNext) throws IOException {
        try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION)) {
            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setTemperature(0.3f)
                    .setMaxOutputTokens(1024) // Increased max tokens for documentation
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
                        String text = extractGeneratedDocumentation(response);
                        if (text != null && !text.isEmpty()) {
                            onNext.accept(text);
                        }
                    });
        }
    }

    public static String extractGeneratedDocumentation(GenerateContentResponse response) {
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