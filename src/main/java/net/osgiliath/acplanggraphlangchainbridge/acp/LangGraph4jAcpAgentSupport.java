package net.osgiliath.acplanggraphlangchainbridge.acp;

import com.agentclientprotocol.model.ContentBlock;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java implementation of ACP Agent logic using LangChain4j.
 */
@Component
public class LangGraph4jAcpAgentSupport implements InAcpAdapter {
    private static final Logger log = LoggerFactory.getLogger(LangGraph4jAcpAgentSupport.class);
    private final LangGraph4jAdapter adapter;

    /**
     * Constructor for LangGraph4jAcpAgentSupport.
     *
     * @param adapter the LangGraph4jAdapter instance to use for processing prompts
     */
    public LangGraph4jAcpAgentSupport(LangGraph4jAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public AgentInfoBridge getAgentInfo() {
        return new AgentInfoBridge("CodePromptAgent", "1.0.0");
    }

    @Override
    public AcpSessionBridge createSession(String sessionId, String cwd, Map<String, String> mcpServers) {
        SessionContext sessionContext = SessionContext.of(sessionId, cwd, mcpServers);
        log.info("Creating new ACP session: {} in {}", sessionContext.sessionId(), sessionContext.cwd());
        return new LangChain4jSession(sessionContext, adapter);
    }

    private static final class LangChain4jSession implements AcpSessionBridge {

        private final SessionContext sessionContext;
        private final LangGraph4jAdapter adapter;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        LangChain4jSession(SessionContext sessionContext, LangGraph4jAdapter adapter) {
            this.sessionContext = sessionContext;
            this.adapter = adapter;
        }

        @Override
        public String getSessionId() {
            return sessionContext.sessionId();
        }

        @Override
        public AtomicBoolean cancelledFlag() {
            return cancelled;
        }

        @Override
        public void cancel() {
            log.info("Cancellation requested for session {}", sessionContext.sessionId());
            cancelledFlag().set(true);
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
                adapter.streamPrompt(sessionContext, promptText, resourceLinks, consumer, cancelled);
            } catch (Exception e) {
                consumer.onError(e);
            }
        }
    }
}
