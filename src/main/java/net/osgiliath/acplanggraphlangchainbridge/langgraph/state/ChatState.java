package net.osgiliath.acplanggraphlangchainbridge.langgraph.state;

import dev.langchain4j.data.message.ChatMessage;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.serializer.AcpLangChain4jStateSerializer;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatState extends MessagesState<ChatMessage> {

    public static final String ATTACHMENTS_META = "attachmentsMeta";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            MESSAGES_STATE, Channels.appender(ArrayList::new),
            ATTACHMENTS_META, Channels.appender(ArrayList::new)
    );

    public ChatState(Map<String, Object> initData) {
        super(initData);
    }

    public static StateSerializer<ChatState> serializer() {
        return new AcpLangChain4jStateSerializer<>(ChatState::new);
    }

    public Optional<String> next() {
        return this.value("next");
    }

    public List<ResourceLinkContent> attachmentsMetadata() {
        return this.<List<ResourceLinkContent>>value(ATTACHMENTS_META).orElse(List.of());
    }
}
