Feature: ACP ResourceLinks Attachment Handling
  As an ACP client communicating with the LangGraph4j adapter
  I want to pass ResourceLinks (file references) along with prompts
  So that the adapter and graph nodes can access and process referenced resources

  Background:
    Given the ACP bridge is initialized
    And the LangGraph4j adapter is available
    And the graph can access attachment metadata
    And the test dataset directory exists at "src/test/resources/dataset"

  # Scenario 1: Basic ResourceLink is passed through the bridge
  Scenario: Bridge accepts and forwards a single ResourceLink with a prompt
    Given I have a prompt "Analyze the attached code file"
    And I have a ResourceLink pointing to file "src/test/resources/dataset/Thread.java" with name "Thread.java"
    And the ResourceLink has mimeType "text/java"
    When I call the bridge with the prompt and ResourceLink
    Then the adapter receives the ResourceLink
    And the ResourceLink is added to the graph state
    And the graph state contains attachments metadata with the ResourceLink name
    And the stream processing completes successfully

  # Scenario 2: Multiple ResourceLinks are handled
  Scenario: Bridge accepts and forwards multiple ResourceLinks
    Given I have a prompt "Compare these two Java files"
    And I have ResourceLinks pointing to dataset files:
      | file                                    | name        | mimeType   |
      | src/test/resources/dataset/Thread.java  | Thread.java | text/java  |
      | src/test/resources/dataset/String.java  | String.java | text/java  |
      | src/test/resources/dataset/configuration.json | Array.json | application/json |
    When I call the bridge with the prompt and all ResourceLinks
    Then the adapter receives all 3 ResourceLinks
    And the graph state contains all 3 attachment metadata entries
    And each ResourceLink retains its name and URI
    And the stream processing completes successfully

  # Scenario 3: ResourceLink with complete metadata
  Scenario: ResourceLink with all metadata fields is preserved in the adapter
    Given I have a prompt "Process the attachment"
    And I have a ResourceLink pointing to file "src/test/resources/dataset/configuration.json"
    And the ResourceLink has name "prod-config.json"
    And the ResourceLink has description "Production configuration settings"
    And the ResourceLink has mimeType "application/json"
    When I call the bridge with the prompt and ResourceLink
    Then the adapter receives the ResourceLink
    And the ResourceLink name is preserved as "prod-config.json"
    And the ResourceLink URI points to "src/test/resources/dataset/configuration.json"
    And the ResourceLink description is preserved as "Production configuration settings"
    And the ResourceLink mimeType is preserved as "application/json"
    And the stream processing completes successfully

  # Scenario 4: ResourceLinks are injected into graph state
  Scenario: ResourceLinks are available in the graph state during execution
    Given I have a prompt "Use the attachment metadata"
    And I have a ResourceLink with name "data.xml" and URI "file:///app/data.xml"
    When I call the bridge with the prompt and ResourceLink
    And the graph executes
    Then the graph state property "attachmentsMeta" contains the ResourceLink
    And the graph nodes can access the ResourceLink from the state
    And the ResourceLink is available throughout the graph execution
    And the stream processing completes successfully

  # Scenario 5: Empty ResourceLink list is handled gracefully
  Scenario: Bridge handles prompts without ResourceLinks
    Given I have a prompt "Simple prompt without attachments"
    And I have an empty ResourceLink list
    When I call the bridge with the prompt and empty ResourceLink list
    Then the adapter processes the prompt normally
    And the graph state attachments metadata is empty
    And the stream processing completes successfully
    And the response is generated without errors

  # Scenario 6: ResourceLink with file URI scheme is processed
  Scenario: ResourceLink with file:// URI scheme is preserved
    Given I have a prompt "Read the file reference"
    And I have a ResourceLink with URI "file:///path/to/source.java"
    When I call the bridge with the prompt and ResourceLink
    Then the adapter preserves the file URI scheme
    And the URI remains "file:///path/to/source.java"
    And the URI is passed to graph nodes
    And the stream processing completes successfully

  # Scenario 7: ResourceLink with http URI scheme is processed
  Scenario: ResourceLink with http:// URI scheme is preserved
    Given I have a prompt "Fetch and analyze the remote resource"
    And I have a ResourceLink with URI "http://example.com/api/resource.json"
    And the ResourceLink has mimeType "application/json"
    When I call the bridge with the prompt and ResourceLink
    Then the adapter preserves the http URI scheme
    And the URI remains "http://example.com/api/resource.json"
    And the mimeType is available to graph nodes
    And the stream processing completes successfully

  # Scenario 8: ResourceLink metadata is logged during processing
  Scenario: ResourceLink details are logged by the adapter for debugging
    Given I have a prompt "Test logging of ResourceLink"
    And I have a ResourceLink with name "Application.java"
    And the ResourceLink has URI "file:///project/src/Application.java"
    When I call the bridge with the prompt and ResourceLink
    And the adapter processes the request
    Then the adapter logs the ResourceLink name
    And the adapter logs the ResourceLink URI
    And the adapter logs the total number of ResourceLinks
    And the stream processing completes successfully

  # Scenario 9: ResourceLink from nested archive is handled
  Scenario: ResourceLink pointing into a ZIP archive is processed correctly
    Given I have a prompt "Analyze Java source from SDK"
    And I have a ResourceLink with name "Thread.java"
    And the ResourceLink has URI "file:///Users/charliemordant/.sdkman/candidates/java/current/lib/src.zip!/java.base/java/lang/Thread.java"
    And the ResourceLink has mimeType "text/java"
    When I call the bridge with the prompt and ResourceLink
    Then the adapter receives the ResourceLink with nested archive path
    And the URI contains the ZIP file path and internal entry path
    And the adapter preserves the complete URI with !/ separator
    And the stream processing completes successfully

    # Alternative: Use dataset file to simulate archive reference
  Scenario: ResourceLink with archive-like reference to dataset file
    Given I have a prompt "Test archive-like resource reference"
    And I have a ResourceLink pointing to file "src/test/resources/dataset/Thread.java" with name "Thread.java"
    And the ResourceLink has mimeType "text/java"
    When I call the bridge with the prompt and ResourceLink
    Then the adapter receives the ResourceLink
    And the URI points to valid dataset file
    And the stream processing completes successfully

  # Scenario 10: ResourceLinks are not modified by the adapter
  Scenario: Adapter preserves ResourceLink integrity without modification
    Given I have a prompt "Process with resource integrity check"
    And I have 2 ResourceLinks with specific properties:
      | name         | uri                              | mimeType       |
      | service.java | file:///api/Service.java        | text/java      |
      | config.yaml  | file:///config/app.yaml         | application/x-yaml |
    When I call the bridge with the prompt and ResourceLinks
    Then the adapter does not modify the ResourceLink names
    And the adapter does not modify the ResourceLink URIs
    And the adapter does not modify the ResourceLink mimeTypes
    And the ResourceLinks are passed to the graph state unmodified
    And the stream processing completes successfully

  # Scenario 11: ResourceLink enables context awareness in the agent
  Scenario: Agent can reference ResourceLinks in its responses
    Given I have a prompt "Tell me about the attached Java class"
    And I have a ResourceLink with name "Observable.java"
    And the ResourceLink has description "Java Observable class"
    When I call the bridge with the prompt and ResourceLink
    And the graph processes the request
    Then the graph state contains the ResourceLink metadata
    And the agent nodes can access the ResourceLink information
    And the stream response may reference the attached resource
    And the stream processing completes successfully

  # Scenario 12: ResourceLink state channel is properly appended
  Scenario: Multiple ResourceLinks are accumulated in the graph state
    Given the graph state has an attachmentsMeta channel with appender semantics
    And I have a prompt with initial ResourceLink "file1.java"
    When I call the bridge with the prompt and first ResourceLink
    And I process the request through the graph
    Then the graph state accumulates the first ResourceLink
    And subsequent graph operations can access this ResourceLink
    And if more ResourceLinks are added, they are appended to the list
    And the stream processing completes successfully

  # Scenario 13: ResourceLink integration with streaming consumer
  Scenario: ResourceLinks are available before streaming begins
    Given I have a prompt "Stream response with resource context"
    And I have a ResourceLink with name "context.json"
    And I have a TokenConsumer that collects streamed tokens
    When I call streamPrompt with the prompt and ResourceLink
    Then the ResourceLink is available to the graph before first token
    Then the adapter injects ResourceLink into state before streaming
    And tokens are streamed with resource context available
    And the TokenConsumer receives all tokens
    And the stream completes successfully

  # Scenario 14: ResourceLink without optional fields is handled
  Scenario: ResourceLink with minimal fields (name and uri) is processed
    Given I have a prompt "Minimal resource link test"
    And I have a ResourceLink with only:
      | field | value                    |
      | name  | minimal.txt             |
      | uri   | file:///tmp/minimal.txt |
    When I call the bridge with the prompt and ResourceLink
    Then the adapter accepts the minimal ResourceLink
    And the ResourceLink name and uri are preserved
    And null fields are handled gracefully
    And the stream processing completes successfully

  # Scenario 15: Bridge method signature includes ResourceLinks parameter
  Scenario: AcpSessionBridge.streamPrompt accepts ResourceLinks parameter
    Given the ACP bridge session is created
    When I inspect the streamPrompt method signature
    Then the method accepts: String promptText
    And the method accepts: List<ContentBlock.ResourceLink> resourceLinks
    And the method accepts: TokenConsumer consumer
    And all parameters are properly forwarded to the adapter






