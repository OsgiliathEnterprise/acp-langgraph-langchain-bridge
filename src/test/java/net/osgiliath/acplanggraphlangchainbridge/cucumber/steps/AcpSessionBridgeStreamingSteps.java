package net.osgiliath.acplanggraphlangchainbridge.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for ACP Session Bridge streaming scenarios.
 * Tests the end-to-end flow from AcpSessionBridge through LangGraph4jAdapter
 * down to the LLMProcessorNode's TokenStream mock.
 */
public class AcpSessionBridgeStreamingSteps {

    private final List<String> streamedTokens = new ArrayList<>();
    private final AtomicReference<Throwable> streamError = new AtomicReference<>();
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AcpAgentSupportBridge bridge;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LangGraph4jAdapter adapter;
    private AcpAgentSupportBridge.AcpSessionBridge session;
    private CountDownLatch streamingComplete;

    @Given("the streaming bridge and adapter are initialized")
    public void theStreamingBridgeAndAdapterAreInitialized() {
        assertThat(bridge).isNotNull();
        assertThat(adapter).isNotNull();
    }

    @Given("an active streaming session")
    public void anActiveStreamingSession() {
        session = bridge.createSession("stream-test-session", ".", Collections.emptyMap());
        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isEqualTo("stream-test-session");
        streamedTokens.clear();
        streamError.set(null);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Sends the prompt through the LangGraph4jAdapter (end-to-end:
     * adapter → PromptGraph → LLMProcessorNode → TokenStream mock).
     */
    private void streamViaAdapter(String prompt) {
        streamingComplete = new CountDownLatch(1);

        adapter.streamPrompt(prompt, Collections.emptyList(), new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                streamedTokens.add(token);
            }

            @Override
            public void onComplete() {
                streamingComplete.countDown();
            }

            @Override
            public void onError(Throwable error) {
                streamError.set(error);
                streamingComplete.countDown();
            }
        });

        try {
            boolean completed = streamingComplete.await(10, TimeUnit.SECONDS);
            assertThat(completed)
                .as("Streaming should complete within timeout")
                .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Streaming interrupted", e);
        }
    }

    // ── When ─────────────────────────────────────────────────────────────

    @When("I send a prompt {string} to the session")
    public void iSendAPromptToTheSession(String prompt) {
        streamViaAdapter(prompt);
    }

    @When("I send a multi-part prompt to the session")
    public void iSendAMultiPartPromptToTheSession() {
        streamViaAdapter("Process this in multiple parts");
    }

    @When("I stream a prompt through the bridge")
    public void iStreamAPromptThroughTheBridge() {
        streamViaAdapter("Stream test prompt");
    }

    // ── Then ─────────────────────────────────────────────────────────────

    @Then("the session should stream the partial response {string}")
    public void theSessionShouldStreamThePartialResponse(String expectedToken) {
        assertThat(streamedTokens)
            .as("Should have streamed at least one token")
            .isNotEmpty();
        assertThat(streamedTokens)
            .as("Should contain the partial response token: " + expectedToken)
            .contains(expectedToken);
    }

    @Then("the session should complete with the final response {string}")
    public void theSessionShouldCompleteWithTheFinalResponse(String expectedFinalToken) {
        assertThat(streamError.get())
            .as("Streaming should not have errored")
            .isNull();
        String full = String.join("", streamedTokens);
        assertThat(full)
            .as("Full streamed response should contain: " + expectedFinalToken)
            .contains(expectedFinalToken);
    }

    @Then("the stream should emit tokens in order")
    public void theStreamShouldEmitTokensInOrder() {
        assertThat(streamedTokens)
            .as("Should have streamed tokens")
            .isNotEmpty();
        // First partial token must appear before any later tokens
        assertThat(streamedTokens.get(0))
            .as("First streamed token should be the partial response")
            .isEqualTo("inprogress");
    }

    @Then("the final response should contain {string}")
    public void theFinalResponseShouldContain(String expectedContent) {
        String concatenated = String.join("", streamedTokens);
        assertThat(concatenated)
            .as("Final streamed response should contain: " + expectedContent)
            .contains(expectedContent);
    }


    @Then("the streaming session should not error")
    public void theStreamingSessionShouldNotError() {
        assertThat(streamError.get())
            .as("Streaming should not produce errors")
            .isNull();
    }

    @Then("the completion handler should be called")
    public void theCompletionHandlerShouldBeCalled() {
        assertThat(streamingComplete.getCount())
            .as("Completion handler should have been called")
            .isZero();
    }

    @Then("the final message should be present in the state")
    public void theFinalMessageShouldBePresentInTheState() {
        assertThat(streamedTokens)
            .as("Should have streamed at least one token")
            .isNotEmpty();

        String finalMessage = String.join("", streamedTokens);
        assertThat(finalMessage)
            .as("Final message should be non-empty")
            .isNotEmpty();
    }
}


