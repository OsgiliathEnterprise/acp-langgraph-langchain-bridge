package com.example.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application class for the ERP system.
 * This class serves as the entry point for the application.
 *
 * Key characteristics:
 * - Uses Spring Boot framework
 * - Implements Dependency Injection pattern
 * - Follows Convention over Configuration principle
 * - Uses Component Scanning for auto-configuration
 *
 * Design patterns used:
 * - Singleton pattern (Spring Bean management)
 * - Factory pattern (Bean creation)
 * - Inversion of Control (IoC)
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.example.erp")
public class Application {

    /**
     * Main entry point for the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Creates a configuration bean.
     * Design pattern: Factory pattern
     *
     * @return application configuration
     */
    @Bean
    public ApplicationConfig applicationConfig() {
        return new ApplicationConfig();
    }

    /**
     * Configuration class for application settings.
     */
    public static class ApplicationConfig {
        private String version = "2.0.0";
        private String environment = "production";

        public String getVersion() {
            return version;
        }

        public String getEnvironment() {
            return environment;
        }
    }
}

