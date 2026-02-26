# ACP ↔ LangGraph / LangChain Bridge

A Spring Boot application that bridges the **JetBrains Agent Client Protocol (ACP)** with **LangChain4j** and **LangGraph4j**, enabling any ACP-compatible IDE (e.g. JetBrains IDEs) to interact with a LangChain4j-powered AI agent over standard I/O.

## Overview

```
┌──────────────┐   stdio (JSON-RPC)   ┌───────────────────────────────────────────────┐
│  ACP Client  │ ◄──────────────────► │  acp-langraph-langchain-bridge                │
│  (IDE / CLI) │                      │                                               │
└──────────────┘                      │  ┌─────────────────┐   ┌──────────────────┐   │
                                      │  │ AcpAgentRunner   │──►│ LangChain4j      │   │
                                      │  │ (Kotlin, ACP SDK)│   │ AgentSupport     │   │
                                      │  └────────┬────────┘   └────────┬─────────┘   │
                                      │           │                     │              │
                                      │           ▼                     ▼              │
                                      │  ┌─────────────────┐   ┌──────────────────┐   │
                                      │  │ Koog ACP SDK     │   │ LangGraph4jAdapter│   │
                                      │  │ (protocol layer) │   │ (orchestrator)   │   │
                                      │  └─────────────────┘   └────────┬─────────┘   │
                                      │                                 │              │
                                      │                                 ▼              │
                                      │                        ┌──────────────────┐   │
                                      │                        │ PromptGraph      │   │
                                      │                        │ (StateGraph)     │   │
                                      │                        └──────────────────┘   │
                                      └───────────────────────────────────────────────┘
```

The bridge has two clearly separated halves:

| Layer | Language | Responsibility |
|---|---|---|
| **ACP transport** (`AcpAgentRunner`) | Kotlin | Speaks the ACP protocol over stdio using the official JetBrains ACP SDK. Translates ACP session/prompt events into calls on the Java side. |
| **Agent orchestration** (`LangChain4jAgentSupport` → `LangGraph4jAdapter` → `PromptGraph`) | Java | Runs the actual LLM agent logic using LangChain4j models and a LangGraph4j state graph. Streams tokens back through a callback interface. |

## Key Components

### `AcpAgentRunner` (Kotlin)

Entry point that boots the ACP server. It:

1. Creates a `StdioTransport` (reads JSON-RPC from stdin, writes to stdout).
2. Registers an `AgentSupport` implementation that delegates to the Java `AcpAgentSupportBridge`.
3. Converts ACP `prompt()` calls into a Kotlin `Flow<Event>`, forwarding streamed tokens as `AgentMessageChunk` events and signalling completion with a `PromptResponse`.

### `AcpAgentSupportBridge` (Java interface)

Defines the contract between the Kotlin ACP layer and the Java agent layer:

- **`getAgentInfo()`** – returns the agent name and version advertised to ACP clients.
- **`createSession(sessionId, cwd, mcpServers)`** – creates a new session.
- **`AcpSessionBridge`** – per-session interface with `processPrompt()` (async, non-streaming) and `streamPrompt()` (streaming via `TokenConsumer` callbacks).

### `LangChain4jAgentSupport` (Java)

Spring `@Component` implementing `AcpAgentSupportBridge`. Creates sessions backed by the `LangGraph4jAdapter`. The non-streaming `processPrompt()` is internally built on top of `streamPrompt()`, accumulating tokens into a single `CompletableFuture<String>`.

### `LangGraph4jAdapter` (Java)

The core orchestrator. For each prompt it:

1. Builds a `StateGraph<ChatState>` via the injected `PromptGraph`.
2. Compiles the graph and calls `app.stream(...)`.
3. Iterates the resulting `NodeOutput` / `StreamingOutput` sequence:
   - **`StreamingOutput` chunks** are forwarded to the ACP `TokenConsumer` in real time.
   - **Regular `NodeOutput` snapshots** (state transitions) are silently consumed.
4. Signals `onComplete()` or `onError()` when the graph finishes.

### `PromptGraph` (Java interface)

Abstraction for the LangGraph4j state graph definition. Implementations wire up nodes (e.g. an *agent* node that calls an LLM, a *tools* node) and conditional edges (e.g. route back to the agent when tool calls are requested, or route to `END`).

### `ChatState` (Java)

Extends LangGraph4j's `MessagesState<ChatMessage>`, carrying the conversation history through the graph. Includes a serializer for state persistence and an optional `next` field used for routing decisions.

## Streaming Architecture

```
ACP Client ──prompt──► AcpAgentRunner ──streamPrompt──► LangGraph4jAdapter
                                                              │
                                                     graph.buildGraph()
                                                     app.stream(userMessage)
                                                              │
                                                              ▼
                                               ┌──────────────────────────┐
                                               │  LangGraph4j Runtime     │
                                               │  ┌──────┐   ┌────────┐  │
                                               │  │ Agent │──►│ Tools  │  │
                                               │  │ Node  │◄──│ Node   │  │
                                               │  └──────┘   └────────┘  │
                                               │  StreamingOutput chunks  │
                                               └────────────┬─────────────┘
                                                             │
                              onNext(token) ◄────────────────┘
                              onComplete()
                                  │
                                  ▼
ACP Client ◄──AgentMessageChunk──AcpAgentRunner
ACP Client ◄──PromptResponse────AcpAgentRunner
```

1. The `agent` node returns `_streaming_messages` backed by a `StreamingChatGenerator`.
2. The LangGraph4j runtime yields `StreamingOutput` chunks interleaved with `NodeOutput` state snapshots.
3. A **conditional edge** (`routeMessage`) inspects the last message: if it contains tool-call requests the graph loops back to execute tools; otherwise it routes to `END`.
4. `LangGraph4jAdapter` forwards each chunk to the `TokenConsumer`, which in turn emits ACP `AgentMessageChunk` events over stdio.

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 | Primary language for agent logic |
| **Kotlin** | 2.1.10 | ACP SDK integration (required by Koog SDK) |
| **Spring Boot** | 3.4.2 | Dependency injection, application lifecycle |
| **JetBrains ACP SDK** (`com.agentclientprotocol:acp`) | 0.15.3 | Official Agent Client Protocol implementation |
| **LangChain4j** | 1.11.0 | LLM abstraction (chat models, tools, memory) |
| **LangGraph4j** | 1.8.3 | Stateful agent graph orchestration |
| **OpenAI (via LangChain4j)** | — | Default LLM provider |

## Building

```bash
./gradlew build
```

## Running

The application communicates over **stdio** (stdin/stdout), which is the transport expected by ACP clients.

```bash
./gradlew bootRun
```

Or run the fat JAR directly:

```bash
java -jar build/libs/acp-langraph-langchain-bridge-1.0-SNAPSHOT.jar
```

> **Note:** All log output is directed to **stderr** (see `logback.xml`) so that stdout remains clean for ACP JSON-RPC messages.

## Testing

The project uses **Cucumber / Gherkin** BDD tests to validate the bridge behaviour:

```bash
./gradlew test
```

Feature files are located in `src/test/resources/features/`:

- **`acp-agent-support-bridge.feature`** – Validates session lifecycle, prompt handling, resource listing, and ping/pong.
- **`acp_streaming.feature`** – Validates token-by-token streaming, sequential ordering, and completion signalling.

## Extending

To plug in your own agent logic, implement the `PromptGraph` interface and register it as a Spring `@Component`. The graph definition determines the agent's behaviour (which LLM to call, which tools to expose, how to route between nodes).

```java
@Component
public class MyGraph implements PromptGraph {
    @Override
    public StateGraph<ChatState> buildGraph() throws GraphStateException {
        // Define nodes, edges, and conditional routing
    }
}
```

## Build

```bash
./gradlew clean build publishToMavenLocal
```

