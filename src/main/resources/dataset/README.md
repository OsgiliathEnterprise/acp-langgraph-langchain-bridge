# Test Dataset Files

This directory contains test data files used in Cucumber BDD scenarios for testing the ACP ResourceLinks attachment support feature.

## Purpose

These files are designed to produce **predictable and accurate LLM responses** for CI/CD testing. Each file contains clear patterns, design principles, and annotations that enable LLMs to provide consistent answers during automated testing.

## Files

### Java Source Files

#### `Thread.java`
- **Type**: Java class demonstrating concurrency
- **Design Patterns**: Observer pattern
- **Key Features**: Runnable implementation, thread-safe operations
- **Expected LLM Recognition**: 
  - Should identify Observer pattern
  - Should recognize concurrent programming concepts
  - Should note thread safety mechanisms

#### `String.java`
- **Type**: Java class demonstrating immutability
- **Design Patterns**: Immutable Value Object, Builder pattern
- **Key Features**: Immutable design, defensive copying, builder for construction
- **Expected LLM Recognition**:
  - Should identify Builder pattern
  - Should recognize immutability principles
  - Should note defensive copying for thread safety

#### `Application.java`
- **Type**: Spring Boot application entry point
- **Design Patterns**: Singleton, Factory, Inversion of Control (IoC)
- **Framework**: Spring Boot
- **Key Features**: Bean configuration, component scanning, dependency injection
- **Expected LLM Recognition**:
  - Should identify Spring Boot application
  - Should recognize Dependency Injection pattern
  - Should note Factory pattern in Bean creation

#### `Service.java`
- **Type**: Business service layer class
- **Design Patterns**: Service Layer, Transaction Script, Repository pattern
- **Framework**: Spring Framework with transaction management
- **Key Features**: Transactional operations, dependency injection, business logic
- **Expected LLM Recognition**:
  - Should identify Service Layer pattern
  - Should recognize Transaction management
  - Should note Repository pattern usage

#### `Controller.java`
- **Type**: REST API controller
- **Design Patterns**: MVC (Model-View-Controller), Front Controller, DTO, Command pattern
- **Framework**: Spring Web MVC
- **Key Features**: RESTful endpoints (GET, POST, PUT, DELETE), HTTP status codes
- **Expected LLM Recognition**:
  - Should identify MVC pattern
  - Should recognize RESTful API design
  - Should note CRUD operations
  - Should identify DTO pattern

### Configuration Files

#### `configuration.json`
- **Type**: JSON configuration file
- **Content**: Application settings for production environment
- **Key Sections**: server, database, features, security, logging
- **Expected LLM Recognition**:
  - Should parse JSON structure correctly
  - Should identify production configuration
  - Should recognize standard configuration patterns (database, security, logging)
  - Should note specific values (port 8080, PostgreSQL database, OAuth2 authentication)

#### `config.yaml`
- **Type**: YAML configuration file (Spring Boot format)
- **Content**: Comprehensive Spring Boot application configuration
- **Key Sections**: application, server, spring (datasource, jpa, security), logging, management
- **Expected LLM Recognition**:
  - Should parse YAML structure correctly
  - Should identify Spring Boot configuration patterns
  - Should recognize Hibernate/JPA configuration
  - Should note OAuth2 security setup
  - Should identify Prometheus metrics export

## Testing Guidelines

### For CI/CD Tests

When using these files in automated tests, expect LLMs to:

1. **Correctly identify file types** and programming languages
2. **Recognize design patterns** explicitly mentioned in comments
3. **Parse configuration files** accurately
4. **Provide consistent answers** about the content
5. **Reference specific elements** like class names, method names, pattern names

### Validation Criteria

Tests should verify that the LLM:
- Mentions specific design patterns (Observer, Builder, MVC, etc.)
- Identifies frameworks correctly (Spring Boot, Spring MVC)
- Parses configuration values accurately
- Provides consistent answers across multiple test runs
- References correct file/class names when discussing the code

## File Updates

When modifying these files:
1. Keep comments clear and explicit about patterns used
2. Maintain predictable structure for consistent LLM interpretation
3. Update this README with any significant changes
4. Ensure changes won't cause test flakiness

## Related Feature Files

These dataset files are referenced in:
- `codeprompt/src/test/resources/features/attachment-support.feature`
- `acp-langraph-langchain-bridge/src/test/resources/features/acp_attachment.feature`

