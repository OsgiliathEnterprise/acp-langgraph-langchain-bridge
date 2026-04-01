import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.ideaExt)
    id("idea")
    alias(libs.plugins.kotlinSerialization)
    `java-library`
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.kotlinJvm)
    wrapper
    id("maven-publish")
    jacoco
}

fun Project.secret(name: String): String? =
    (findProperty(name) as String?) ?: System.getenv(name)

group = "net.osgiliath.ai"
description = "Bridge module between ACP and LangGraph/LangChain"
version = (findProperty("releaseVersion") as String?) ?: "1.0-SNAPSHOT"
tasks.wrapper {
    gradleVersion = "9.4.1"
    distributionType = Wrapper.DistributionType.BIN
}

configure<JacocoPluginExtension> {
    toolVersion = libs.versions.jacoco.get()
}

tasks.withType<Test>().configureEach {
    // Attach JaCoCo agent to every test task
    configure<JacocoTaskExtension> {
        isEnabled = true
    }
    finalizedBy(tasks.named("jacocoTestReport"))

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

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}
// Override Spring Boot's JUnit version to match Cucumber requirements
ext {
    set("junit-jupiter.version", libs.versions.junitJupiter.get())
}

// Explicitly configure Java toolchain for this module to ensure consistency
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

// Configure Kotlin to use the same Java toolchain
kotlin {
    jvmToolchain(21)
}

configurations.all {
    resolutionStrategy {
        force(libs.kotlinStdlib)
        force(libs.kotlinStdlibCommon)
        force(libs.kotlinxCoroutinesCore)
        force(libs.kotlinxCoroutinesCoreJvm)
        // Force JUnit Platform to match Cucumber requirements
        force(libs.junitPlatformSuite)
        force(libs.junitPlatformLauncher)
        force(libs.junitJupiter)
    }
}

dependencies {
    implementation(platform(libs.cucumberBom))
    implementation(platform(libs.langgraph4jBom))
    implementation(platform(libs.langchain4jBom))

    // Official ACP Kotlin SDK from JetBrains
    implementation(libs.acp)

    // Kotlin stdlib (required by ACP SDK)
    implementation(libs.kotlinStdlib)
    implementation(libs.kotlinStdlibCommon)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinxCoroutinesCoreJvm)

    // LangChain4j Backend (Agent Orchestrator)
    implementation(libs.langchain4j)
    implementation(libs.langchain4jSpringBootStarter)
    implementation(libs.langchain4jOpenAiSpringBootStarter)
    implementation(libs.langchain4jHttpClientJdk)

    // LangGraph4j (Agent State Management)
    implementation(libs.langgraph4jCore)
    implementation(libs.langgraph4jLangchain4j)

    // Spring Boot
    implementation(libs.springBootStarter)
    implementation(libs.springBootStarterJson)

    testImplementation(libs.springBootStarterTest) {
        exclude(group = "org.junit.platform")
    }

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.awaitility)

    // Cucumber/Gherkin BDD Testing
    testImplementation(libs.cucumberCore)
    testImplementation(libs.cucumberJava)
    testImplementation(libs.cucumberSpring)
    testImplementation(libs.cucumberJunitPlatformEngine)
    testImplementation(libs.junitPlatformSuite)
    implementation(kotlin("stdlib"))
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    standardInput = System.`in`
}

// This module is published as a library, not an executable Spring Boot app.
tasks.named("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("acp-langraph-langchain-bridge")
                description.set("Bridge module between ACP and LangGraph/LangChain")
                url.set("https://github.com/OsgiliathEnterprise/acp-langgraph-langchain-bridge")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("charliemordant")
                        name.set("Charlie Mordant")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/OsgiliathEnterprise/acp-langgraph-langchain-bridge.git")
                    developerConnection.set("scm:git:ssh://git@github.com/OsgiliathEnterprise/acp-langgraph-langchain-bridge.git")
                    url.set("https://github.com/OsgiliathEnterprise/acp-langgraph-langchain-bridge")
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}
jreleaser {
    configFile.set(file("jreleaser.yml"))
}

sonar {
    properties {
        // All values injected from env vars / Gradle properties set in CI secrets.
        secret("SONAR_HOST_URL")?.let { property("sonar.host.url", it) }
        secret("SONAR_TOKEN")?.let { property("sonar.token", it) }
        secret("SONAR_ORGANIZATION")?.let { property("sonar.organization", it) }
        secret("SONAR_PROJECT_KEY")?.let { property("sonar.projectKey", it) }
        secret("SONAR_PROJECT_NAME")?.let { property("sonar.projectName", it) }
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml"
        )
        property(
            "sonar.java.binaries",
            "${layout.buildDirectory.get()}/classes/java/main,${layout.buildDirectory.get()}/classes/kotlin/main"
        )
        property(
            "sonar.java.test.binaries",
            "${layout.buildDirectory.get()}/classes/java/test"
        )
        property(
            "sonar.exclusions",
            "src/test/resources/dataset/**"
        )
    }
}
