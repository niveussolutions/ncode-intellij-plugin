plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
    id("jacoco")
    id("org.sonarqube") version "5.0.0.4638"
}

group = "com.technology"
version = "3.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.cloud:google-cloud-vertexai:1.17.0")
    implementation("com.google.cloud:google-cloud-aiplatform:3.32.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.21.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1") // Update JUnit Jupiter API version
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1") // Update JUnit Jupiter Engine version
    testImplementation("org.mockito:mockito-core:4.11.0") // Ensure Mockito core is added
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0") // Ensure Mockito JUnit integration is added
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.1.7")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf())
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        useJUnitPlatform {
            includeEngines("junit-jupiter") // Explicitly include JUnit Jupiter engine
        }
        maxHeapSize = "1G"
        testLogging {
            events("passed", "skipped", "failed") // Ensure all test events are logged
            showStandardStreams = true // Enable standard stream logging
        }
        finalizedBy(jacocoTestReport) // Ensure Jacoco report is generated after tests
    }
    
    jacocoTestReport {
        dependsOn(test) // Ensure tests run before generating the report
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "intellij-plugin")
        property("sonar.projectName", "intellij-plugin")
        property("sonar.host.url", "https://sonar.niveussolutions.com")
        property("sonar.junit.reportPaths", "build/test-results/test") // Ensure test reports path is correct
        property("sonar.scm.provider", "git") // Enable blame information using Git
    }
}