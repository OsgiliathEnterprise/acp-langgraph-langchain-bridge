package net.osgiliath.acplanggraphlangchainbridge.cucumber;

import com.agentclientprotocol.model.ContentBlock;
import io.cucumber.spring.CucumberContextConfiguration;
import net.osgiliath.acplanggraphlangchainbridge.CodePromptFrameworkApplication;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.graph.PromptGraph;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cucumber Spring configuration that sets up the Spring Boot context for BDD tests.
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = CodePromptFrameworkApplication.class,
    properties = {
        "spring.main.web-application-type=none"
    }
)
public class CucumberSpringConfiguration {

    /**
     * Mock CommandLineRunners to prevent them from starting and blocking stdin.
     */
    @MockitoBean
    private CommandLineRunner commandLineRunner;

    /**
     * Provide a mock AcpAgentSupportBridge bean for testing.
     */
    @Bean
    public AcpAgentSupportBridge acpAgentSupportBridge() {
        return new AcpAgentSupportBridge() {
            @Override
            public AgentInfoBridge getAgentInfo() {
                return new AgentInfoBridge("Test Agent", "1.0");
            }

            @Override
            public AcpSessionBridge createSession(String sessionId, String cwd, Map<String, String> mcpServers) {
                return new AcpSessionBridge() {
                    @Override
                    public String getSessionId() {
                        return sessionId;
                    }

                    @Override
                    public CompletableFuture<String> processPrompt(String promptText, java.util.List<ContentBlock.ResourceLink> resourceLinks) {
                        return CompletableFuture.completedFuture("Mock response: " + promptText);
                    }

                    @Override
                    public void streamPrompt(String promptText, java.util.List<ContentBlock.ResourceLink> resourceLinks, TokenConsumer consumer) {
                        consumer.onNext("Mock");
                        consumer.onNext(" response");
                        consumer.onComplete();
                    }
                };
            }
        };
    }

    /**
     * Provide a LangGraph4jAdapter bean for testing.
     */
    @Bean
    public LangGraph4jAdapter langChain4jAdapter(PromptGraph promptGraph) {
        return new LangGraph4jAdapter(promptGraph) {
            @Override
            public void streamPrompt(String promptText, java.util.List<ContentBlock.ResourceLink> resourceLinks, AcpAgentSupportBridge.TokenConsumer consumer) {
                if ("Hello AI, please respond with exactly the word 'ACK'".equals(promptText)) {
                    consumer.onNext("ACK");
                    consumer.onComplete();
                    return;
                }
                super.streamPrompt(promptText, resourceLinks, consumer);
            }
        };
    }
}
