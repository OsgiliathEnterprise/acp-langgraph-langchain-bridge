Feature: ACP Agent Support Bridge
  As an ACP server implementation
  I want to bridge between ACP protocol and LangChain4j
  So that I can provide agent support capabilities

  Background:
    Given the ACP Agent Support Bridge is initialized

  Scenario: Initialize ACP session
    When an ACP client sends an initialize request
    Then the bridge should respond with server capabilities
    And the session should be established

  Scenario: Handle prompt request from ACP client
    Given an active ACP session
    When the client sends a prompt "What is your purpose?"
    Then the bridge should forward the prompt to LangChain4j
    And stream the response back to the client
    And the client should receive completion notification

  Scenario: List available resources
    Given an active ACP session
    When the client requests the resource list
    Then the bridge should return available resources
    And resources should include project files
    And resources should include configuration files

  Scenario: Read a specific resource
    Given an active ACP session
    And a resource "build.gradle.kts" exists
    When the client requests to read the resource
    Then the bridge should return the resource content
    And the content should not be empty

  Scenario: List available tools
    Given an active ACP session
    When the client requests the tool list
    Then the bridge should return available tools
    And tools should include AI assistant capabilities

  Scenario: Handle ping request
    Given an active ACP session
    When the client sends a ping request
    Then the bridge should respond with acknowledgment
    And the session should remain active

