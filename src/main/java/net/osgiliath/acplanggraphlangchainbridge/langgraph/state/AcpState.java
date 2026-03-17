package net.osgiliath.acplanggraphlangchainbridge.langgraph.state;

import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.serializer.AcpLangChain4jStateSerializer;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.*;

/**
 * ChatState is a state class that extends MessagesState with ChatMessage as the message type. It represents the state of a chat conversation, including the messages exchanged and any attachments sent by the user. The state includes channels for storing the chat messages, the metadata of the attachments, and the content of the attachments. This state is used in the context of a chat application where users can send messages and attachments, and it allows for tracking the conversation history and associated files.
 */
public class AcpState<T> extends MessagesState<T> {

    public static final String SESSION_CONTEXT = "sessionContext";
    /**
     * Channel for attachments metadata. The content of this channel is a list of {@link ResourceLinkContent}, which contains the metadata of the attachments sent by the user.
     */
    public static final String ATTACHMENTS_META = "attachmentsMeta";
    /**
     * Channel for attachments content. The content of this channel is a list of byte arrays, which contains the content of the attachments sent by the user.
     */
    public static final String ATTACHMENTS_SCHEMA = "attachments";

    /**
     * State schema for the {@link AcpState}. This defines the channels that are used in the state and their types. The schema is a map where the keys are the channel names and the values are the channel definitions. In this case, we have three channels: MESSAGES_STATE, ATTACHMENTS_META, and ATTACHMENTS. The MESSAGES_STATE channel is defined in the parent class and is used to store the chat messages. The ATTACHMENTS_META channel is used to store the metadata of the attachments sent by the user, and the ATTACHMENTS channel is used to store the content of the attachments sent by the user.
     */
    public static final Map<String, Channel<?>> SCHEMA = getSchema();

    private static Map<String, Channel<?>> getSchema() {
        Map<String, Channel<?>> result = new HashMap<>(MessagesState.SCHEMA);
        result.put(SESSION_CONTEXT, Channels.base((currentValue, newValue) -> newValue, SessionContext::empty));
        result.put(ATTACHMENTS_META, Channels.appender(ArrayList::new));
        result.put(ATTACHMENTS_SCHEMA, Channels.appender(ArrayList::new));
        return result;
    }

    /**
     * Constructor for the {@link AcpState}. It takes a map of initial data, which is used to initialize the state. The map should contain the initial values for the channels defined in the state schema.
     * @param initData A map of initial data for the state. The keys should correspond to the channel names defined in the state schema, and the values should be the initial values for those channels.
     */
    public AcpState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * Serializer for the {@link AcpState}. Uses the {@link AcpLangChain4jStateSerializer} with a constructor reference to create new instances of {@link AcpState}.
     * @return  A ChatState serializer that can be used to serialize and deserialize ChatState instances.
     */
    public static <T> StateSerializer<AcpState<T>> serializer() {
        return new AcpLangChain4jStateSerializer<>(AcpState::new);
    }

    public SessionContext sessionContext() {
        return this.<SessionContext>value(SESSION_CONTEXT).orElse(SessionContext.empty());
    }

    public String sessionId() {
        return sessionContext().sessionId();
    }

    public String cwd() {
        return sessionContext().cwd();
    }

    public Map<String, String> mcpServers() {
        return sessionContext().mcpServers();
    }

    /**
     * Gets the next message in the chat. This is used to determine if there are more messages to process in the chat. If there are no more messages, it returns an empty Optional.
     * @return An Optional containing the next message in the chat, or an empty Optional if there are no more messages to process.
     */
    public Optional<String> next() {
        return this.value("next");
    }

    /**
     * Gets the metadata of the attachments sent by the user. This is used to determine if there are any attachments to process in the chat. If there are no attachments, it returns an empty list.
     * @return A list of ResourceLinkContent containing the metadata of the attachments sent by the user, or an empty list if there are no attachments to process.
     */
    public List<ResourceLinkContent> attachmentsMetadata() {
        return this.<List<ResourceLinkContent>>value(ATTACHMENTS_META).orElse(List.of());
    }

    /**
     * Gets the content of the attachments sent by the user. This is used to determine if there are any attachments to process in the chat. If there are no attachments, it returns an empty list.
     * @return A list of byte arrays containing the content of the attachments sent by the user, or an empty list if there are no attachments to process.
     */
    public List<byte[]> attachments() {
        return this.<List<byte[]>>value(ATTACHMENTS_SCHEMA).orElse(List.of());
    }
}
