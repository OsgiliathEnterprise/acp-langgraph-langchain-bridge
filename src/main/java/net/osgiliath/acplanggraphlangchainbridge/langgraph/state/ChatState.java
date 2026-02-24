package net.osgiliath.acplanggraphlangchainbridge.langgraph.state;

import dev.langchain4j.data.message.ChatMessage;
import org.bsc.langgraph4j.langchain4j.serializer.std.LC4jStateSerializer;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.serializer.StateSerializer;

import java.util.Map;
import java.util.Optional;

public class ChatState extends MessagesState<ChatMessage> {
    public ChatState(Map<String, Object> initData) {
        super( initData  );
    }

    public static StateSerializer<ChatState> serializer() {
        return new LC4jStateSerializer<>( ChatState::new );
    }

    public Optional<String> next() {
        return this.value("next");
    }
}
