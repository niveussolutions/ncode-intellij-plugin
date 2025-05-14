package com.technology.ncode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.technology.ncode.config.EnvironmentConfig;

/**
 * Handles reporting usage metrics to the backend server.
 * Makes asynchronous HTTP requests to avoid blocking the UI thread.
 */
public class UsageMetricsReporter {
    private static final Logger LOG = Logger.getInstance(UsageMetricsReporter.class);
    private static final String EXTENSION_TYPE = determineExtensionType();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final String API_URL = EnvironmentConfig.USAGE_METRICS_API_URL;
    private static final String USAGE_METRICS_SECRET_KEY = EnvironmentConfig.USAGE_METRICS_SECRET_KEY;
    static{
        System.out.println("API_URL (env usagemetric): " + API_URL);
        System.out.println("USAGE_METRICS_SECRET_KEY (env usagemetric): " + USAGE_METRICS_SECRET_KEY);
    }
    // Singleton HTTP client for better performance
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    /**
     * Simple container class for user identification information.
     */
    public static class MetricsUserInfo {
        public final String email;
        public final String projectId;

        public MetricsUserInfo(String email, String projectId) {
            this.email = email != null ? email : "";
            this.projectId = projectId != null ? projectId : "";
        }
    }

    /**
     * Reports usage metrics asynchronously to the backend service.
     * 
     * @param editor        The current editor instance
     * @param generatedText The text that was suggested
     * @param wasAccepted   Whether the suggestion was accepted by the user
     * @return CompletableFuture representing the asynchronous operation
     */
    public static CompletableFuture<Void> reportEditorMetrics(
            com.intellij.openapi.editor.Editor editor,
            String generatedText,
            boolean wasAccepted) {

        // Count lines in the generated text for metrics reporting
        int linesOfCodeSuggested = countLines(generatedText);

        // For accepted suggestions, all lines were accepted
        // For rejected suggestions, zero lines were accepted
        int linesOfCodeAccepted = wasAccepted ? linesOfCodeSuggested : 0;

        // Get user information
        MetricsUserInfo userInfo = getUserInfo();

        // Report metrics asynchronously
        return reportMetricsAsync(
                userInfo.email,
                userInfo.projectId,
                linesOfCodeSuggested,
                linesOfCodeAccepted,
                editor.getProject(),
                "completion");
    }

    /**
     * Reports explanation metrics asynchronously to the backend service.
     * 
     * @param editor The current editor instance
     * @return CompletableFuture representing the asynchronous operation
     */
    public static CompletableFuture<Void> reportExplanationMetrics(
            com.intellij.openapi.editor.Editor editor) {
        // Get user information
        MetricsUserInfo userInfo = getUserInfo();

        // Report metrics asynchronously
        return reportMetricsAsync(
                userInfo.email,
                userInfo.projectId,
                0, // linesOfCodeSuggested
                0, // linesOfCodeAccepted
                editor.getProject(),
                "explanation");
    }

    /**
     * Reports usage metrics asynchronously to the backend service.
     * 
     * @param email                User email (optional, can be empty)
     * @param projectId            Project identifier (optional, can be empty)
     * @param linesOfCodeSuggested Number of lines suggested
     * @param linesOfCodeAccepted  Number of lines accepted
     * @param project              Current project (for displaying notifications)
     * @param requestType          Type of request (completion or explanation)
     * @return CompletableFuture representing the asynchronous operation
     */
    public static CompletableFuture<Void> reportMetricsAsync(
            String email,
            String projectId,
            int linesOfCodeSuggested,
            int linesOfCodeAccepted,
            Project project,
            String requestType) {

        // Use "niveus-ncode" as default value if email is empty
        String emailToUse = (email == null || email.isEmpty()) ? "niveus-ncode" : email;
        // Use "niveus-ncode" as default value if projectId is empty
        String projectIdToUse = (projectId == null || projectId.isEmpty()) ? "niveus-ncode" : projectId;

        // Create the JSON payload
        String jsonPayload = String.format(
                "{\"secretKey\":\"%s\",\"email\":\"%s\",\"projectId\":\"%s\"," +
                        "\"requestType\":\"%s\",\"extensionType\":\"%s\"," +
                        "\"linesOfCodeSuggested\":%d,\"linesOfCodeAccepted\":%d}",
                USAGE_METRICS_SECRET_KEY,
                emailToUse,
                projectIdToUse,
                requestType,
                EXTENSION_TYPE,
                linesOfCodeSuggested,
                linesOfCodeAccepted);

        // Create the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Execute the request asynchronously and handle the response
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    System.out.println("Metrics API returned status code: " + statusCode);
                    if (statusCode < 200 || statusCode >= 300) {
                        LOG.warn("Metrics API returned status code: " + statusCode);
                    }
                })
                .exceptionally(e -> {
                    // Log the error but don't let it affect the user experience
                    LOG.warn("Failed to report metrics", e);
                    return null;
                });
    }

    /**
     * Gets the user email and project ID with default values if not available.
     * 
     * @return A MetricsUserInfo object containing the email and project ID
     */
    public static MetricsUserInfo getUserInfo() {
        String projectId = EnvironmentConfig.VERTEX_PROJECT_ID;

        // Get the email from "gcloud auth list"
        String email = getEmailFromGcloudAuthList();

        // If email is empty, use "niveus-ncode"
        if (email.isEmpty()) {
            email = "niveus-ncode";
        }

        return new MetricsUserInfo(email, projectId);
    }

    /**
     * Calculates the number of lines in a text string.
     * 
     * @param text The text to count lines in
     * @return The number of lines in the text
     */
    public static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Count newlines plus 1 for the last line (which might not end with a newline)
        return (int) text.chars().filter(ch -> ch == '\n').count() + 1;
    }

    /**
     * Determines the current IDE extension type.
     * 
     * @return The extension type as a string
     */
    private static String determineExtensionType() {
        String ideName = System.getProperty("idea.platform.prefix");
        if (ideName != null) {
            switch (ideName) {
                case "PyCharm":
                    return "pycharm";
                case "Idea":
                    return "intellij";
                // Add more cases as needed for other JetBrains IDEs
                default:
                    return "intellij";
            }
        }
        return "intellij";
    }

    /**
     * Retrieves a value from the Google Cloud CLI.
     * 
     * @param valueType The type of value to retrieve (e.g., "project", "account")
     * @return The retrieved value, or an empty string if retrieval failed
     */
    public static String getValueFromGcloud(String valueType) {
        try {
            Process process = new ProcessBuilder("gcloud", "config", "get-value", valueType)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line.trim());
                }
                process.waitFor();

                String result = output.toString();
                if (!result.isEmpty() && !result.contains("ERROR")) {
                    return result;
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("Failed to get " + valueType + " from Google CLI", e);
        }
        // Ensure an empty string is returned for invalid inputs or errors
        return "";
    }

    /**
     * Retrieves the email from the Google Cloud CLI using "gcloud auth list".
     * 
     * @return The retrieved email, or an empty string if retrieval failed
     */
    static String getEmailFromGcloudAuthList() {
        try {
            Process process = new ProcessBuilder("gcloud", "auth", "list")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                String email = "";
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("*")) {
                        email = line.substring(1).trim();
                        break;
                    }
                }
                process.waitFor();
                return email;
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("Failed to get email from Google CLI", e);
        }
        return "";
    }
}