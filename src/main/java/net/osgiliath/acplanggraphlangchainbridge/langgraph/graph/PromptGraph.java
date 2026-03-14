package net.osgiliath.acplanggraphlangchainbridge.langgraph.graph;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;

/**
 * Interface for defining a prompt graph. A prompt graph is a specific type of graph that represents the flow of a prompt in the system.
 * Implementing classes should provide the logic to build the graph according to the requirements of the prompt.
 * @param <S> the type of the state that this graph will use. It must extend AgentState, which is a common base class for states in the LangGraph framework.
 */
public interface PromptGraph<S extends AgentState> {
    /**
     * Builds the graph for the prompt. This method is called by the framework when the graph is being initialized.
     * @return the built graph
     * @throws GraphStateException if there is an error building the graph, such as invalid node configuration or state issues
     */
    StateGraph<S> buildGraph() throws GraphStateException;
}
