package net.osgiliath.acplanggraphlangchainbridge.langgraph.state;

import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStateSessionContextTest {

    @Test
    void exposesSessionContextValuesFromState() {
        SessionContext sessionContext = SessionContext.of("session-123", "/workspace", Map.of("mcp-a", "http://localhost:3000"));
        AcpState<ChatMessage> state = new AcpState<>(Map.of(AcpState.SESSION_CONTEXT, sessionContext));

        assertThat(state.sessionContext()).isEqualTo(sessionContext);
        assertThat(state.sessionId()).isEqualTo("session-123");
        assertThat(state.cwd()).isEqualTo("/workspace");
        assertThat(state.mcpServers()).containsExactlyEntriesOf(Map.of("mcp-a", "http://localhost:3000"));
    }

    @Test
    void fallsBackToEmptySessionContextWhenMissing() {
        AcpState<ChatMessage> state = new AcpState<>(Map.of());

        assertThat(state.sessionContext()).isEqualTo(SessionContext.empty());
        assertThat(state.sessionId()).isEmpty();
        assertThat(state.cwd()).isEqualTo(".");
        assertThat(state.mcpServers()).isEmpty();
    }
}
