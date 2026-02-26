package net.osgiliath.acplanggraphlangchainbridge.acp;

import com.agentclientprotocol.model.ContentBlock;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Java implementation of ACP Agent logic using LangChain4j.
 */
@Component
public class LangGraph4jAcpAgentSupport implements AcpAgentSupportBridge {
    private static final Logger log = LoggerFactory.getLogger(LangGraph4jAcpAgentSupport.class);
    private final LangGraph4jAdapter adapter;

    public LangGraph4jAcpAgentSupport(LangGraph4jAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public AgentInfoBridge getAgentInfo() {
        return new AgentInfoBridge("CodePromptAgent", "1.0.0");
    }

    @Override
    public AcpSessionBridge createSession(String sessionId, String cwd, Map<String, String> mcpServers) {
        log.info("Creating new ACP session: {} in {}", sessionId, cwd);
        return new LangChain4jSession(sessionId, adapter);
    }

    private record LangChain4jSession(String sessionId, LangGraph4jAdapter adapter) implements AcpSessionBridge {

        @Override
        public String getSessionId() {
            return sessionId;
        }


        @Override
        public CompletableFuture<String> processPrompt(String promptText, List<ContentBlock.ResourceLink> resourceLinks) {
            CompletableFuture<String> future = new CompletableFuture<>();
            StringBuilder sb = new StringBuilder();
            streamPrompt(promptText, resourceLinks, new TokenConsumer() {
                @Override
                public void onNext(String token) {
                    sb.append(token);
                }

                @Override
                public void onComplete() {
                    future.complete(sb.toString());
                }

                @Override
                public void onError(Throwable error) {
                    future.completeExceptionally(error);
                }
            });
            return future;
        }


        @Override
        public void streamPrompt(String promptText, List<ContentBlock.ResourceLink> resourceLinks, TokenConsumer consumer) {
            try {
                adapter.streamPrompt(promptText, resourceLinks, consumer);
            } catch (Exception e) {
                consumer.onError(e);
            }
        }
    }
}
