package net.osgiliath.acplanggraphlangchainbridge.langgraph.serializer;

import dev.langchain4j.data.message.Content;
import org.bsc.langgraph4j.langchain4j.serializer.std.ContentSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.LC4jStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;

/**
 * Custom state serializer for ACP that extends the standard LC4jStateSerializer. It overrides the default content serializer to use a custom implementation
 * @param <STATE> the type of the state that this serializer will handle. It must extend AgentState, which is a common base class for states in the LangGraph framework.
 */
public class AcpLangChain4jStateSerializer<STATE extends AgentState> extends LC4jStateSerializer<STATE> {

    /**
     * Constructor for AcpLangChain4jStateSerializer.
     * @param stateFactory a factory function that creates new instances of the state. This is used by the serializer to create new state instances during deserialization.
     */
    public AcpLangChain4jStateSerializer(AgentStateFactory<STATE> stateFactory) {
        super(stateFactory);
        mapper().unregister(ContentSerializer.class);
        mapper().register(Content.class, new AcpBridgeContentSerializer());
    }
}
