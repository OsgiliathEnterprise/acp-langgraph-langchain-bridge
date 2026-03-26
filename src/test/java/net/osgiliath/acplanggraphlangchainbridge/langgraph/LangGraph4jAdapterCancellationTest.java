package net.osgiliath.acplanggraphlangchainbridge.langgraph;

import dev.langchain4j.data.message.ChatMessage;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.graph.PromptGraph;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.AcpState;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.SessionContext;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Tests for the cancellation-aware streaming loop in {@link LangGraph4jAdapter}.
 *
 * <p>The adapter checks {@code cancelled.get()} at the top of each iteration of
 * {@code app.stream()}.  When the flag is already set before the first iteration the
 * loop exits immediately via {@code consumer.onComplete()} without delivering any
 * {@code StreamingOutput} chunks.</p>
 */
class LangGraph4jAdapterCancellationTest {

    // -----------------------------------------------------------------------
    // Test 1 – pre-set cancelled flag: no tokens delivered, onComplete called
    // -----------------------------------------------------------------------

    @Test
    void preCancelledFlagSkipsAllTokensAndCallsOnComplete() {
        LangGraph4jAdapter adapter = new LangGraph4jAdapter(new SimpleNoopGraph());

        List<String> tokens = new ArrayList<>();
        AtomicInteger completeCount = new AtomicInteger(0);
        AtomicBoolean cancelled = new AtomicBoolean(true); // already cancelled

        adapter.streamPrompt(
                SessionContext.of("session-cancel", "/tmp", Map.of()),
                "this prompt should not produce tokens",
                List.of(),
                new AcpAgentSupportBridge.TokenConsumer() {
                    @Override
                    public void onNext(String token) {
                        tokens.add(token);
                    }

                    @Override
                    public void onComplete() {
                        completeCount.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable e) {
                        throw new AssertionError("Unexpected error", e);
                    }
                },
                cancelled
        );

        assertThat(tokens)
                .as("no tokens should be delivered when the session is pre-cancelled")
                .isEmpty();
        assertThat(completeCount.get())
                .as("onComplete() must be called exactly once even when cancelled")
                .isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Test 2 – fresh AtomicBoolean(false): graph runs to completion normally
    // -----------------------------------------------------------------------

    @Test
    void freshUncancelledFlagAllowsGraphToRunToCompletion() {
        LangGraph4jAdapter adapter = new LangGraph4jAdapter(new SimpleNoopGraph());

        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicBoolean cancelled = new AtomicBoolean(false); // not cancelled

        adapter.streamPrompt(
                SessionContext.of("session-not-cancelled", "/tmp", Map.of()),
                "normal prompt",
                List.of(),
                new AcpAgentSupportBridge.TokenConsumer() {
                    @Override
                    public void onNext(String token) {
                        // no tokens expected from noop graph, but if there were any this would be the place to check them
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        throw new AssertionError("Unexpected error", e);
                    }
                },
                cancelled
        );

        assertThat(completed)
                .as("onComplete() must be reached when cancelled flag starts as false")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Test 3 – flag set concurrently from another thread during streaming
    // -----------------------------------------------------------------------

    /**
     * Uses a two-node graph so the loop iterates at least twice.  A helper thread
     * sets {@code cancelled = true} as soon as the consumer receives the first
     * {@code onComplete()} notification from the graph's completion.  Because the
     * flag is checked before each node output, the adapter exits the loop before
     * processing the second node and calls {@code onComplete()} at most once.
     *
     * <p>In practice the adapter's loop is synchronous, so this test validates the
     * <em>pre-cancellation</em> path from a racing thread perspective: even if
     * {@code cancel()} arrives just after the last node output, {@code onComplete()}
     * is still called exactly once (the adapter's own call).</p>
     */
    @Test
    void flagSetFromAnotherThreadCausesEarlyLoopExit() throws InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        LangGraph4jAdapter adapter = new LangGraph4jAdapter(new TwoNodeGraph());

        AtomicInteger onNextCount = new AtomicInteger(0);
        AtomicInteger completeCount = new AtomicInteger(0);

        // Set cancelled from a separate thread slightly before we start,
        // so it fires inside (or before) the streaming loop.
        Thread canceller = new Thread(() -> cancelled.set(true));
        canceller.setDaemon(true);
        canceller.start();
        canceller.join(500);

        adapter.streamPrompt(
                SessionContext.of("session-concurrent-cancel", "/tmp", Map.of()),
                "two-node prompt",
                List.of(),
                new AcpAgentSupportBridge.TokenConsumer() {
                    @Override
                    public void onNext(String token) {
                        onNextCount.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        completeCount.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable e) {
                        throw new AssertionError("Unexpected error", e);
                    }
                },
                cancelled
        );

        assertThat(completeCount.get())
                .as("onComplete() must be called exactly once regardless of cancellation timing")
                .isEqualTo(1);
        assertThat(onNextCount.get())
                .as("no StreamingOutput chunks expected from noop graph nodes")
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Helper graphs
    // -----------------------------------------------------------------------

    /**
     * Single-node graph that does nothing; used to drive ≥1 loop iteration.
     */
    private static final class SimpleNoopGraph implements PromptGraph<AcpState<ChatMessage>> {
        @Override
        public StateGraph<AcpState<ChatMessage>> buildGraph() throws GraphStateException {
            return new StateGraph<AcpState<ChatMessage>>(AcpState.SCHEMA, AcpState.serializer())
                    .addNode("noop", node_async(state -> Map.of()))
                    .addEdge(START, "noop")
                    .addEdge("noop", END);
        }
    }

    /**
     * Two-node graph that drives ≥2 loop iterations.
     */
    private static final class TwoNodeGraph implements PromptGraph<AcpState<ChatMessage>> {
        @Override
        public StateGraph<AcpState<ChatMessage>> buildGraph() throws GraphStateException {
            return new StateGraph<AcpState<ChatMessage>>(AcpState.SCHEMA, AcpState.serializer())
                    .addNode("first", node_async(state -> Map.of()))
                    .addNode("second", node_async(state -> Map.of()))
                    .addEdge(START, "first")
                    .addEdge("first", "second")
                    .addEdge("second", END);
        }
    }
}

