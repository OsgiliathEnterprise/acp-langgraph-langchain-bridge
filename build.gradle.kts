import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.9"
    id("idea")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.10"
}

group = "net.osgiliath.prompt"
version = "1.0-SNAPSHOT"
tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        // Add detailed test logging for debugging
        testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }

        // Enable debug output
        systemProperty("java.util.logging.config.file", "")
    }
// Override Spring Boot's JUnit version to match Cucumber 7.34.2 requirements
ext {
    set("junit-jupiter.version", "5.14.2")
}

// Explicitly configure Java toolchain for this module to ensure consistency
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Configure Kotlin to use the same Java toolchain
kotlin {
    jvmToolchain(21)
}

// Configure Kotlin compiler options
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        // Ensure compatibility with Java-only dependencies
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

springBoot {
    mainClass.set("net.osgiliath.acplangraphlangchainbridge.CodePromptFrameworkApplication")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.1.10")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
        // Force JUnit Platform 5.14.2 to match Cucumber 7.34.2 requirements
        force("org.junit.platform:junit-platform-engine:1.14.2")
        force("org.junit.platform:junit-platform-commons:1.14.2")
        force("org.junit.platform:junit-platform-suite:1.14.2")
        force("org.junit.platform:junit-platform-suite-api:1.14.2")
        force("org.junit.platform:junit-platform-suite-engine:1.14.2")
        force("org.junit.platform:junit-platform-launcher:1.14.2")
        force("org.junit.jupiter:junit-jupiter:5.14.2")
        force("org.junit.jupiter:junit-jupiter-api:5.14.2")
        force("org.junit.jupiter:junit-jupiter-engine:5.14.2")
    }
}

dependencies {
    "implementation"(platform("io.cucumber:cucumber-bom:7.34.2"))
    "implementation"(platform("org.bsc.langgraph4j:langgraph4j-bom:1.8.3"))
    "implementation"(platform("dev.langchain4j:langchain4j-bom:1.11.0"))

    // Official ACP Kotlin SDK from JetBrains
    // Provides built-in protocol handling, STDIO transport, and session management
    implementation("com.agentclientprotocol:acp:0.15.3")

    // Kotlin stdlib (required by ACP SDK)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:2.1.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")

    // LangChain4j Backend (Agent Orchestrator)
    implementation("dev.langchain4j:langchain4j")
    implementation("dev.langchain4j:langchain4j-spring-boot-starter")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot-starter")
    implementation("dev.langchain4j:langchain4j-http-client-jdk")

    // LangGraph4j (Agent State Management)
    implementation("org.bsc.langgraph4j:langgraph4j-core")
    implementation("org.bsc.langgraph4j:langgraph4j-langchain4j")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        // Exclude Spring Boot's JUnit Platform version management
        exclude(group = "org.junit.platform")
    }

    // Import JUnit BOM AFTER Spring Boot to override its version management
    testImplementation(platform("org.junit:junit-bom:5.14.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    // Testing utilities
    testImplementation("org.awaitility:awaitility:4.2.2")

    // Cucumber/Gherkin BDD Testing
    testImplementation("io.cucumber:cucumber-core")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.junit.platform:junit-platform-suite")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    standardInput = System.`in`
}

// Publishing configuration for local Maven repository
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "net.osgiliath.prompt"
            artifactId = "acp-langraph-langchain-bridge"
            version = "1.0-SNAPSHOT"
        }
    }
    repositories {
        mavenLocal()
    }
}

