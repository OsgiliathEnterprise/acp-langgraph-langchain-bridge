package net.osgiliath.acplanggraphlangchainbridge.langgraph.state;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable execution context shared across the ACP session, graph state, and downstream tool or MCP calls.
 */
public record SessionContext(String sessionId, String cwd, Map<String, String> mcpServers) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SessionContext EMPTY = new SessionContext("", ".", Map.of());

    public SessionContext {
        sessionId = sessionId == null ? "" : sessionId;
        cwd = cwd == null || cwd.isBlank() ? "." : cwd;
        mcpServers = mcpServers == null || mcpServers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(mcpServers));
    }

    public static SessionContext of(String sessionId, String cwd, Map<String, String> mcpServers) {
        return new SessionContext(sessionId, cwd, mcpServers);
    }

    /**
     * Returns an empty SessionContext with default values. Useful for testing or when no context is needed.
     *
     * @return an empty SessionContext instance
     */
    public static SessionContext empty() {
        return EMPTY;
    }
}
