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
        property("sonar.sources", "src/main/java/com/technology/ncode/VertexAI")
        property("sonar.tests", "src/test/java/com/technology/ncode/VertexAI")
        property("sonar.java.test.binaries", "${project.buildDir}/classes/java/test")
        property("sonar.coverage.exclusions", "**/generated/**,**/*Form.java,**/*Dialog.java,**/AskAQuestion/**,**/GenerateDocumentation/**,**/GenerateTest/**,**/GoogleThis/**,**/InlineCodeCompletion/**,**/VertexAIChatbot.java")
        property("sonar.verbose", "true")
        property("sonar.inclusions", "**/VertexAI/**")
    }
}

group = "com.technology"
version = "4.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation("com.google.cloud:google-cloud-vertexai:1.17.0")
    implementation("com.google.cloud:google-cloud-aiplatform:3.32.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.21.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    implementation("com.fifesoft:rsyntaxtextarea:3.3.2")
    implementation("de.sciss:jsyntaxpane:1.0.0")
    implementation("de.sciss:syntaxpane:1.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
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
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
        extensions.configure(JacocoTaskExtension::class.java) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
      }
    }

    jacocoTestReport {
        dependsOn(test) // Ensure tests run before generating the report
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        
        classDirectories.setFrom(sourceSets.main.get().output.asFileTree.matching {
            include("**/VertexAI/**")
            exclude(
                "**/test/**",
                "**/generated/**",
                "**/*Form*",
                "**/*Dialog*",
                "**/*ToolWindowContent*",
                "**/*ToolWindowFactory*",
                "**/AskAQuestion/**",
                "**/GenerateDocumentation/**",
                "**/GenerateTest/**",
                "**/GoogleThis/**",
                "**/InlineCodeCompletion/**"
            )
        })
        
        executionData.from(fileTree(project.buildDir) {
            include("jacoco/test.exec") // Ensure execution data is captured
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