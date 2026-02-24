package net.osgiliath.acplanggraphlangchainbridge.configuration;

import net.osgiliath.acplanggraphlangchainbridge.edge.LLMToToolEdge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.graph.PromptGraph;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.ChatState;
import net.osgiliath.acplanggraphlangchainbridge.node.LLMProcessorNode;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Configuration
public class TestGraph implements PromptGraph {
    private final LLMToToolEdge edge;
    private final LLMProcessorNode node;

    public TestGraph(LLMToToolEdge edge, LLMProcessorNode node) {
        this.edge = edge;
        this.node = node;
    }
    @Override
    public StateGraph buildGraph() throws GraphStateException {
         return new StateGraph<>(ChatState.SCHEMA, ChatState.serializer())
                .addNode("agent", node_async(node))
                .addEdge(START, "agent")
                .addConditionalEdges("agent",
                        edge_async(edge),
                        Map.of("next", "agent", "exit", END));
    }
}
