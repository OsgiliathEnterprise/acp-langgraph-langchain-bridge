package net.osgiliath.acplanggraphlangchainbridge.langgraph.serializer;

import dev.langchain4j.data.message.Content;
import org.bsc.langgraph4j.langchain4j.serializer.std.ContentSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.LC4jStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;

public class AcpLangChain4jStateSerializer<State extends AgentState> extends LC4jStateSerializer<State> {
    public AcpLangChain4jStateSerializer(AgentStateFactory<State> stateFactory) {
        super(stateFactory);
        mapper().unregister(ContentSerializer.class);
        mapper().register(Content.class, new AcpBridgeContentSerializer());
    }
}
