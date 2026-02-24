Feature: ACP Session Bridge Streaming to LangChain4j TokenStream
  As a client using the ACP protocol,
  I want to send a prompt through the ACP Session Bridge
  And receive streamed responses token-by-token through LangChain4j's TokenStream
  So that I can handle real-time AI assistant responses

  Background:
    Given the streaming bridge and adapter are initialized
    And an active streaming session

  Scenario: Stream a prompt through ACP Session Bridge to TokenStream
    When I send a prompt "test message" to the session
    Then the session should stream the partial response "inprogress"
    And the session should complete with the final response "done"

  Scenario: Multiple tokens streamed sequentially
    When I send a multi-part prompt to the session
    Then the stream should emit tokens in order
    And the final response should contain "done"

  Scenario: Stream response completes successfully
    When I stream a prompt through the bridge
    Then the streaming session should not error
    And the completion handler should be called
    And the final message should be present in the state

