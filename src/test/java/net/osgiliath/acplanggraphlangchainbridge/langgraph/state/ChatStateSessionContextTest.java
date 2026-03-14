package net.osgiliath.acplanggraphlangchainbridge.langgraph.state;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStateSessionContextTest {

    @Test
    void exposesSessionContextValuesFromState() {
        SessionContext sessionContext = SessionContext.of("session-123", "/workspace", Map.of("mcp-a", "http://localhost:3000"));
        ChatState state = new ChatState(Map.of(ChatState.SESSION_CONTEXT, sessionContext));

        assertThat(state.sessionContext()).isEqualTo(sessionContext);
        assertThat(state.sessionId()).isEqualTo("session-123");
        assertThat(state.cwd()).isEqualTo("/workspace");
        assertThat(state.mcpServers()).containsExactlyEntriesOf(Map.of("mcp-a", "http://localhost:3000"));
    }

    @Test
    void fallsBackToEmptySessionContextWhenMissing() {
        ChatState state = new ChatState(Map.of());

        assertThat(state.sessionContext()).isEqualTo(SessionContext.empty());
        assertThat(state.sessionId()).isEmpty();
        assertThat(state.cwd()).isEqualTo(".");
        assertThat(state.mcpServers()).isEmpty();
    }
}
