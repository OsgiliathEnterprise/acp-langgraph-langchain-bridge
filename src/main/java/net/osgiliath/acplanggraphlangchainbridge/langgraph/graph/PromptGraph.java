package net.osgiliath.acplanggraphlangchainbridge.langgraph.graph;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

public interface PromptGraph {
    StateGraph buildGraph() throws GraphStateException;
}
