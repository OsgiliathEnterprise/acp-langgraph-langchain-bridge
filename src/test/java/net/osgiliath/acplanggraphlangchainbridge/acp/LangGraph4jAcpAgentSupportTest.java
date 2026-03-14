package net.osgiliath.acplanggraphlangchainbridge.acp;

import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.SessionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LangGraph4jAcpAgentSupportTest {

    @Test
    void processPromptCompletesFutureWhenStreamingCompletes() throws Exception {
        LangGraph4jAdapter adapter = mock(LangGraph4jAdapter.class);
        LangGraph4jAcpAgentSupport support = new LangGraph4jAcpAgentSupport(adapter);

        doAnswer(invocation -> {
            AcpAgentSupportBridge.TokenConsumer consumer = invocation.getArgument(3);
            consumer.onNext("Hello ");
            consumer.onNext("world");
            consumer.onComplete();
            return null;
        }).when(adapter).streamPrompt(any(SessionContext.class), eq("prompt"), any(), any());

        AcpAgentSupportBridge.AcpSessionBridge session = support.createSession("session-1", "/tmp", Map.of("mcp-a", "stdio://server"));
        CompletableFuture<String> result = session.processPrompt("prompt", List.of());

        String response = result.get(1, TimeUnit.SECONDS);

        assertTrue(result.isDone());
        assertEquals("Hello world", response);
        verify(adapter).streamPrompt(
            eq(SessionContext.of("session-1", "/tmp", Map.of("mcp-a", "stdio://server"))),
            eq("prompt"),
            any(),
            any()
        );
    }
}
