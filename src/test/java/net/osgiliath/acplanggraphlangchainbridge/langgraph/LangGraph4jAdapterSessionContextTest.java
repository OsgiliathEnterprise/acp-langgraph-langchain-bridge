package net.osgiliath.acplanggraphlangchainbridge.langgraph;

import dev.langchain4j.data.message.ChatMessage;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.graph.PromptGraph;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.AcpState;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.SessionContext;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

class LangGraph4jAdapterSessionContextTest {

    @Test
    void seedsInitialGraphStateWithSessionContext() throws GraphStateException {
        AtomicReference<SessionContext> capturedContext = new AtomicReference<>();
        LangGraph4jAdapter adapter = new LangGraph4jAdapter(new CapturingPromptGraph(capturedContext));
        AtomicBoolean completed = new AtomicBoolean(false);

        adapter.streamPrompt(
            SessionContext.of("session-777", "/workspace/demo", Map.of("mcp-a", "stdio://server")),
            "hello graph",
            java.util.List.of(),
            new AcpAgentSupportBridge.TokenConsumer() {
                @Override
                public void onNext(String token) {
                    // Completed step is the interesting one
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                }

                @Override
                public void onError(Throwable error) {
                    throw new AssertionError(error);
                }
            }
        );

        assertThat(completed).isTrue();
        assertThat(capturedContext.get()).isEqualTo(SessionContext.of(
            "session-777",
            "/workspace/demo",
            Map.of("mcp-a", "stdio://server")
        ));
    }

    private static final class CapturingPromptGraph implements PromptGraph<AcpState<ChatMessage>> {
        private final AtomicReference<SessionContext> capturedContext;

        private CapturingPromptGraph(AtomicReference<SessionContext> capturedContext) {
            this.capturedContext = capturedContext;
        }

        @Override
        public StateGraph<AcpState<ChatMessage>> buildGraph() throws GraphStateException {
            return new StateGraph<AcpState<ChatMessage>>(AcpState.SCHEMA, AcpState.serializer())
                .addNode("capture", node_async(state -> {
                    capturedContext.set(state.sessionContext());
                    return Map.of();
                }))
                .addEdge(START, "capture")
                .addEdge("capture", END);
        }
    }
}

