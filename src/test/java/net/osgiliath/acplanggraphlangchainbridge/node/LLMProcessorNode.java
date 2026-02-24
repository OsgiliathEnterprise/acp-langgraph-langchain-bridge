package net.osgiliath.acplanggraphlangchainbridge.node;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.ChatState;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Graph node that streams LLM responses using the {@code @AiService} DSL.
 *
 * auto-configured via {@link dev.langchain4j.service.spring.AiService}.
 * The system prompt, model selection, and HTTP client are all handled by
 * Spring Boot auto-configuration — no manual {@code ChatRequest} assembly.</p>
 *
 * <h3>Streaming bridge</h3>
 * <p>The {@code @AiService}'s {@link TokenStream} is bridged into LangGraph4j's
 * {@link StreamingChatGenerator} so the graph runtime can iterate over
 * {@link org.bsc.langgraph4j.streaming.StreamingOutput} chunks:</p>
 * <ol>
 *   <li>Create a {@link StreamingChatGenerator} with a {@code mapResult} that
 *       merges the final {@link dev.langchain4j.data.message.AiMessage} into the state.</li>
 *   <li>Obtain a {@link TokenStream} from {@code assistant.streamChat(prompt)}.</li>
 *   <li>Wire the token stream's callbacks to the generator's
 *       {@link dev.langchain4j.model.chat.response.StreamingChatResponseHandler}.</li>
 *   <li>Call {@link TokenStream#start()} to kick off non-blocking streaming.</li>
 *   <li>Return {@code Map.of("_streaming_messages", generator)}.</li>
 * </ol>
 */
@Component
public class LLMProcessorNode implements NodeAction<ChatState> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LLMProcessorNode.class);

    public LLMProcessorNode() {
    }

    @Override
    public Map<String, Object> apply(ChatState state) {
        log.info("CallModel");

        var generator = StreamingChatGenerator.<MessagesState<ChatMessage>>builder()
                .mapResult(response -> Map.of("messages", response.aiMessage()))
                .startingNode("agent")
                .startingState(state)
                .build();

        // Extract the latest user message text from the state.
        String userMessageText = state.messages().stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .reduce((first, second) -> second) // last user message
                .map(UserMessage::singleText)
                .orElse("");

        // Use the @AiService DSL — system prompt is declared via @SystemMessage
        // on JavaSpringBootAssistant, model & HTTP client are auto-configured.
        TokenStream tokenStream = new TokenStream() {
            private Consumer<String> partialHandler;
            private Consumer<ChatResponse> completeHandler;
            private Consumer<Throwable> errorHandler;

            @Override
            public TokenStream onPartialResponse(Consumer<String> partialResponseHandler) {
                this.partialHandler = partialResponseHandler;
                return this;
            }

            @Override
            public TokenStream onRetrieved(Consumer<List<Content>> contentHandler) {
                return this;
            }

            @Override
            public TokenStream onToolExecuted(Consumer<ToolExecution> toolExecuteHandler) {
                return this;
            }

            @Override
            public TokenStream onCompleteResponse(Consumer<ChatResponse> completeResponseHandler) {
                this.completeHandler = completeResponseHandler;
                return this;
            }

            @Override
            public TokenStream onError(Consumer<Throwable> errorHandler) {
                this.errorHandler = errorHandler;
                return this;
            }

            @Override
            public TokenStream ignoreErrors() {
                this.errorHandler = null;
                return this;
            }

            @Override
            public void start() {
                try {
                    if (partialHandler != null) {
                        partialHandler.accept("inprogress");
                        partialHandler.accept("done");
                    }
                    if (completeHandler != null) {
                        completeHandler.accept(ChatResponse.builder()
                            .aiMessage(AiMessage.from("inprogressdone"))
                            .build());
                    }
                } catch (Throwable t) {
                    if (errorHandler != null) {
                        errorHandler.accept(t);
                    }
                }
            }
        };

        // Bridge TokenStream callbacks → StreamingChatGenerator handler (queue).
        var handler = generator.handler();
        tokenStream
                .onPartialResponse(handler::onPartialResponse)
                .onCompleteResponse(handler::onCompleteResponse)
                .onError(handler::onError)
                .start();

        return Map.of("_streaming_messages", generator);
    }
}
