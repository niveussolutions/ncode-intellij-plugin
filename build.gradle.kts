plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
    id("jacoco")
    id("org.sonarqube") version "5.0.0.4638"
}

jacoco {
    toolVersion = "0.8.8" // Use a recent version of JaCoCo
}

sonarqube {
    properties {
        property("sonar.projectKey", "intellij-plugin")
        property("sonar.projectName", "intellij-plugin")
        property("sonar.host.url", "https://sonar.niveussolutions.com")
        property("sonar.token", "sqp_d357bc7703283a6dd6065e9702d893faef9a0ab9")
        property("sonar.java.binaries", "${project.buildDir}/classes/java/main")
        property("sonar.junit.reportPaths", "${project.buildDir}/test-results/test")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.buildDir}/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")
        property("sonar.java.test.binaries", "${project.buildDir}/classes/java/test")
        property("sonar.coverage.exclusions", "**/generated/**,**/*Form.java,**/*Dialog.java")
        property("sonar.verbose", "true")
    }
}

group = "com.technology"
version = "3.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation("com.google.cloud:google-cloud-vertexai:1.17.0")
    implementation("com.google.cloud:google-cloud-aiplatform:3.32.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.21.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
}

// Configure Gradle IntelliJ Plugin
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
            includeEngines("junit-jupiter")
        }
        maxHeapSize = "1G"
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
    
    jacocoTestReport {
        dependsOn(test) // Ensure tests run before generating the report
        
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
        
        // Configure JaCoCo to exclude UI classes and generated code
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/generated/**",
                        "**/*Form*",
                        "**/*Dialog*",
                        "**/*Ui*"
                    )
                }
            })
        )
        
        // Ensure the report is generated in the correct location
        executionData.from(fileTree(project.buildDir) {
            include("jacoco/test.exec")
        })
    }
    
    // Create a dedicated task to ensure proper task ordering
    register("analyzeCoverage") {
        dependsOn("test", "jacocoTestReport", "sonarqube")
        doLast {
            println("Code coverage analysis completed")
        }
    }
    
    // Configure sonarqube task to run after JaCoCo
    named("sonarqube") {
        dependsOn("jacocoTestReport")
    }
}