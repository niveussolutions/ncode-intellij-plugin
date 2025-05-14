package com.technology.ncode.config;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvironmentConfig {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("/home/user/Development/ncode-intellij-extension").ignoreIfMissing().load();
//    Dotenv.configure().ignoreIfMissing().load();

    // Google Cloud/Vertex AI
    public static final String VERTEX_PROJECT_ID = dotenv.get("VERTEX_PROJECT_ID");
    public static final String VERTEX_LOCATION = dotenv.get("VERTEX_LOCATION");
    public static final String VERTEX_MODEL_ID = dotenv.get("VERTEX_MODEL_ID");
    
    // Usage metrics
    public static final String USAGE_METRICS_API_URL = dotenv.get("USAGE_METRICS_API_URL");
    public static final String USAGE_METRICS_SECRET_KEY = dotenv.get("USAGE_METRICS_SECRET_KEY");

    //SonarQube
    public static final String SONAR_HOST_URL = dotenv.get("SONAR_HOST_URL");
    public static final String SONAR_TOKEN = dotenv.get("SONAR_TOKEN");
    public static final String SONAR_PROJECT_NAME = dotenv.get("SONAR_PROJECT_NAME");
    public static final String SONAR_PROJECT_KEY = dotenv.get("SONAR_PROJECT_KEY");
}