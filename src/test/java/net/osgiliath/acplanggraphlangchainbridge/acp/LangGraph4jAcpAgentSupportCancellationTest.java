package net.osgiliath.acplanggraphlangchainbridge.acp;

import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.SessionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that {@link LangGraph4jAcpAgentSupport} wires the per-session
 * {@code cancelled} {@link AtomicBoolean} correctly so that calling
 * {@link AcpAgentSupportBridge.AcpSessionBridge#cancel()} on a session
 * causes the flag that was passed to {@link LangGraph4jAdapter#streamPrompt}
 * to be {@code true}.
 */
@ExtendWith(MockitoExtension.class)
class LangGraph4jAcpAgentSupportCancellationTest {

    // -----------------------------------------------------------------------
    // Test 1 – cancel() before streamPrompt: adapter receives a pre-set flag
    // -----------------------------------------------------------------------

    private static AcpAgentSupportBridge.TokenConsumer noopConsumer() {
        return new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                // no-op
            }

            @Override
            public void onComplete() {
                // no-op
            }

            @Override
            public void onError(Throwable e) {
                // no-op
            }
        };
    }

    // -----------------------------------------------------------------------
    // Test 2 – cancel() is session-scoped: a second session is not affected
    // -----------------------------------------------------------------------

    @Test
    void cancelBeforeStreamingPassesTrueFlagToAdapter() {
        LangGraph4jAdapter adapter = mock(LangGraph4jAdapter.class);
        LangGraph4jAcpAgentSupport support = new LangGraph4jAcpAgentSupport(adapter);

        AcpAgentSupportBridge.AcpSessionBridge session =
                support.createSession("session-pre-cancel", "/workspace", Map.of());

        // Cancel before any prompt is sent
        session.cancel();

        // Trigger streamPrompt so the flag is forwarded to the adapter
        session.streamPrompt("hello", List.of(), noopConsumer());

        ArgumentCaptor<AtomicBoolean> cancelledCaptor =
                ArgumentCaptor.forClass(AtomicBoolean.class);

        verify(adapter).streamPrompt(
                any(SessionContext.class),
                any(String.class),
                any(List.class),
                any(AcpAgentSupportBridge.TokenConsumer.class),
                cancelledCaptor.capture()
        );

        assertThat(cancelledCaptor.getValue().get())
                .as("the AtomicBoolean passed to the adapter must be true after cancel()")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Test 3 – cancel() during streaming: flag is visible to a concurrent reader
    // -----------------------------------------------------------------------

    @Test
    void cancelOnOneSessionDoesNotAffectAnotherSession() {
        LangGraph4jAdapter adapter = mock(LangGraph4jAdapter.class);
        LangGraph4jAcpAgentSupport support = new LangGraph4jAcpAgentSupport(adapter);

        AcpAgentSupportBridge.AcpSessionBridge sessionA =
                support.createSession("session-A", "/workspace", Map.of());
        AcpAgentSupportBridge.AcpSessionBridge sessionB =
                support.createSession("session-B", "/workspace", Map.of());

        sessionA.cancel(); // only session A is cancelled

        sessionB.streamPrompt("hello from B", List.of(), noopConsumer());

        ArgumentCaptor<AtomicBoolean> cancelledCaptor =
                ArgumentCaptor.forClass(AtomicBoolean.class);

        verify(adapter).streamPrompt(
                any(SessionContext.class),
                any(String.class),
                any(List.class),
                any(AcpAgentSupportBridge.TokenConsumer.class),
                cancelledCaptor.capture()
        );

        assertThat(cancelledCaptor.getValue().get())
                .as("session B's AtomicBoolean must still be false – cancelling A must not affect B")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Simulates what happens when the ACP SDK calls {@code cancel()} on a session
     * while {@code streamPrompt} is in progress on another thread.
     *
     * <p>The adapter mock blocks until a latch is released by the test thread,
     * which calls {@code cancel()} at that point.  After unblocking, the adapter
     * mock reads the flag that was passed to it and records whether it is {@code true}.
     * The test then asserts that the adapter observed the flag as {@code true}.</p>
     */
    @Test
    void cancelDuringStreamingIsVisibleInsideAdapterCall() throws InterruptedException {
        java.util.concurrent.CountDownLatch adapterEnteredLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch cancelCalledLatch = new java.util.concurrent.CountDownLatch(1);
        AtomicBoolean flagSeenInsideAdapter = new AtomicBoolean(false);

        LangGraph4jAdapter adapter = mock(LangGraph4jAdapter.class);
        LangGraph4jAcpAgentSupport support = new LangGraph4jAcpAgentSupport(adapter);

        AcpAgentSupportBridge.AcpSessionBridge session =
                support.createSession("session-concurrent", "/workspace", Map.of());

        // When the mock adapter is called, it signals entry and then waits until
        // the test calls cancel() – then reads the flag.
        doAnswer(invocation -> {
            AtomicBoolean cancelled = invocation.getArgument(4);
            adapterEnteredLatch.countDown();           // signal: inside adapter
            cancelCalledLatch.await(2, java.util.concurrent.TimeUnit.SECONDS); // wait for cancel()
            flagSeenInsideAdapter.set(cancelled.get()); // observe flag mid-call
            AcpAgentSupportBridge.TokenConsumer consumer = invocation.getArgument(3);
            consumer.onComplete();
            return null;
        }).when(adapter).streamPrompt(any(), any(), any(), any(), any(AtomicBoolean.class));

        // Run streamPrompt on a background thread
        Thread streamThread = new Thread(() ->
                session.streamPrompt("prompt", List.of(), noopConsumer())
        );
        streamThread.setDaemon(true);
        streamThread.start();

        // Wait for the adapter to start, then cancel the session
        adapterEnteredLatch.await(2, java.util.concurrent.TimeUnit.SECONDS);
        session.cancel();
        cancelCalledLatch.countDown();

        streamThread.join(2000);

        assertThat(flagSeenInsideAdapter.get())
                .as("the AtomicBoolean observed inside the adapter must be true after concurrent cancel()")
                .isTrue();
    }
}

