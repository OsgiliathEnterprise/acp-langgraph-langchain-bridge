package net.osgiliath.acplanggraphlangchainbridge.it;

import com.agentclientprotocol.model.ContentBlock;
import net.osgiliath.acplanggraphlangchainbridge.AcpLangGraphLangChainBridgeApplication;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = AcpLangGraphLangChainBridgeApplication.class,properties = {
    "spring.main.web-application-type=none"
})
class LangChain4jAdapterIT {
    @Autowired
    private LangGraph4jAdapter adapter;

    // Mock CommandLineRunners to prevent them from starting and blocking stdin
    @MockitoBean
    private CommandLineRunner commandLineRunner;

    /*
    static OllamaContainer ollamaContainer;
    @BeforeAll
    public static void before_all() throws IOException, InterruptedException {
        System.setProperty("api.version", "1.44");
        ollamaContainer = new OllamaContainer(
            DockerImageName.parse("ollama/ollama")
        ).withReuse(true);
        ollamaContainer.start();
        ollamaContainer.execInContainer("ollama", "pull", "gemma3:1b");
    }
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("langchain4j.ollama.chat-model.base-url", () -> "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434));
        registry.add("langchain4j.ollama.streaming-chat-model.base-url", () -> "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434));
    }
    @AfterAll
    public static void after_all() {
        if (ollamaContainer != null) {
            ollamaContainer.stop();
        }
    }
*/
    @Test
    void contextLoads() {
        assertThat(adapter).isNotNull();
    }

    @Test
    void testProcessPromptWithEmptyPrompt() {
        AtomicReference<String> response = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        adapter.streamPrompt("", Collections.EMPTY_LIST,new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                response.set(token);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable t) {}
        });
        await().atMost(5, SECONDS).untilTrue(completed);
        assertThat(response.get()).isEqualTo("Please provide a prompt.");
    }

    @Test
    void testProcessPromptWithBlankPrompt() {
        AtomicReference<String> response = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        adapter.streamPrompt("   ", Collections.EMPTY_LIST, new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                response.set(token);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable t) {}
        });
        await().atMost(5, SECONDS).untilTrue(completed);
        assertThat(response.get()).isEqualTo("Please provide a prompt.");
    }
    
}
