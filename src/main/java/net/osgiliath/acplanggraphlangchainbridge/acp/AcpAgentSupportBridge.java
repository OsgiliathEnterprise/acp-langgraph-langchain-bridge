package net.osgiliath.acplanggraphlangchainbridge.acp;


import com.agentclientprotocol.model.ContentBlock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Java interface for ACP Agent Support.
 * This will be called by the Kotlin AcpAgentRunner to delegate logic to Java.
 */
public interface AcpAgentSupportBridge {
    
    /**
     * Called during ACP initialization.
     * @return Agent implementation name and capabilities.
     */
    AgentInfoBridge getAgentInfo();

    /**
     * Called when a new session is requested.
     */
    AcpSessionBridge createSession(String sessionId, String cwd, Map<String, String> mcpServers);

    interface AcpSessionBridge {
        String getSessionId();
        CompletableFuture<String> processPrompt(String promptText, List<ContentBlock.ResourceLink> resourceLinks);
        void streamPrompt(String promptText, List<ContentBlock.ResourceLink> promtResourceLinks, TokenConsumer consumer);
    }

    interface TokenConsumer {
        void onNext(String token);
        void onComplete();
        void onError(Throwable error);
    }

    record AgentInfoBridge(String name, String version) {
    }
}
