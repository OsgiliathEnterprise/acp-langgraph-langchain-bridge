package net.osgiliath.acplanggraphlangchainbridge.langgraph;

import com.agentclientprotocol.model.ContentBlock;
import dev.langchain4j.data.message.UserMessage;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.graph.PromptGraph;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.ChatState;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Adapter - Bridges Koog ACP facade with LangChain4j orchestrator.
 *
 * <p>Translates between Koog's protocol layer and LangChain4j's business logic.</p>
 *
 * <h3>Streaming architecture</h3>
 * <p>The graph uses the official LangGraph4j streaming pattern:</p>
 * <ol>
 *   <li>The {@code agent} node returns {@code _streaming_messages} with a
 *       {@link org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator}.</li>
 *   <li>The graph runtime iterates the generator, emitting
 *       {@link StreamingOutput} chunks interleaved with regular
 *       {@link org.bsc.langgraph4j.NodeOutput} snapshots.</li>
 *   <li>A <b>conditional edge</b> ({@code routeMessage}) checks the last message:
 *       if it contains tool execution requests the graph loops back; otherwise it
 *       routes to {@code END}.</li>
 *   <li>This adapter iterates {@code app.stream()}, forwards each
 *       {@link StreamingOutput#chunk()} to the ACP {@code TokenConsumer}, and
 *       signals completion when the graph finishes.</li>
 * </ol>
 */
@Component
public class LangGraph4jAdapter {

    private static final Logger log = LoggerFactory.getLogger(LangGraph4jAdapter.class);

    private final PromptGraph graph;

    public LangGraph4jAdapter(
                              PromptGraph graph) {
        this.graph = graph;
    }

    /**
     * Process a prompt using LangChain4j with streaming.
     *
     * <p>Each token is delivered to the {@code consumer} as soon as the model
     * produces it. The method blocks until the full response has been streamed.</p>
     *
     * <p>ResourceLinks are embedded as {@link ResourceLinkContent} in the UserMessage,
     * following LangChain4j's multimodal content pattern.</p>
     */
    public void streamPrompt(String promptText,
                             List<ContentBlock.ResourceLink> resourceLinks,
                             AcpAgentSupportBridge.TokenConsumer consumer) {
        log.debug("Adapter streaming prompt: {}...",
            promptText.length() > 50 ? promptText.substring(0, 50) : promptText);

        if (resourceLinks != null && !resourceLinks.isEmpty()) {
            log.debug("Adapter received {} ResourceLink(s)", resourceLinks.size());
            resourceLinks.forEach(link ->
                log.debug("  - ResourceLink: name={}, uri={}", link.getName(), link.getUri())
            );
        }

        if (promptText == null || promptText.isBlank()) {
            consumer.onNext("Please provide a prompt.");
            consumer.onComplete();
            return;
        }

        // Build the graph with a conditional edge following the official pattern.
        final StateGraph<ChatState> workflow;
        try {
            workflow = graph.buildGraph();
        } catch (GraphStateException e) {
            consumer.onError(e);
            return;
        }

        try {
            var app = workflow.compile();

            // Create UserMessage with only text content
            // ResourceLinks will be stored separately in the state to avoid casting issues
            UserMessage userMessage = UserMessage.from(promptText);

            // Build initial state with the message and separate attachments
            Map<String, Object> initialState = new java.util.HashMap<>();
            initialState.put("messages", userMessage);

            // Store ResourceLinks as ResourceLinkContent in a separate state field
            // This approach avoids issues with LLM systems trying to cast mixed Content types
            if (resourceLinks != null && !resourceLinks.isEmpty()) {
                List<ResourceLinkContent> linkContents = resourceLinks.stream()
                    .map(ResourceLinkContent::from)
                    .toList();

                initialState.put(ChatState.ATTACHMENTS_META, linkContents);
                log.debug("Stored {} ResourceLinkContent(s) in attachmentsMeta state field", linkContents.size());
            }

            // app.stream() yields StreamingOutput (token chunks) interleaved
            // with NodeOutput (state snapshots). We forward only the chunks.
            var states = app.stream(initialState);

            states.forEach(nodeOutput -> {
                if (nodeOutput instanceof StreamingOutput<ChatState> streamingOutput) {
                    var chunk = streamingOutput.chunk();
                    if (chunk != null && !chunk.isEmpty()) {
                        consumer.onNext(chunk);
                    }
                }
                // Regular NodeOutput → state snapshot, nothing to forward
            });

            consumer.onComplete();
        } catch (Throwable t) {
            consumer.onError(t);
        }
    }
}
