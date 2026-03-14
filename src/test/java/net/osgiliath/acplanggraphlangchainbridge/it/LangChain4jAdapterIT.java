package net.osgiliath.acplanggraphlangchainbridge.it;

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
