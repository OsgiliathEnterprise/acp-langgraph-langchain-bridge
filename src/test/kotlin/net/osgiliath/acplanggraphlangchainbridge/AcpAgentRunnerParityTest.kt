package net.osgiliath.acplanggraphlangchainbridge

import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.LATEST_PROTOCOL_VERSION
import com.agentclientprotocol.model.McpServer
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class AcpAgentRunnerParityTest {

    @Test
    fun `initialize advertises protocol version loadSession capability and implementation`() = runBlocking {
        val bridge = RecordingBridge()
        val runner = AcpAgentRunner(bridge)

        val agentInfo = runner.createAgentSupport().initialize(ClientInfo())

        assertEquals(LATEST_PROTOCOL_VERSION, agentInfo.protocolVersion)
        assertTrue(agentInfo.capabilities.loadSession)
        assertEquals("BridgeAgent", agentInfo.implementation?.name)
        assertEquals("9.9.9", agentInfo.implementation?.version)
    }

    @Test
    fun `createSession generates a non-default session id and forwards cwd plus MCP servers`() = runBlocking {
        val bridge = RecordingBridge()
        val runner = AcpAgentRunner(bridge)
        val sessionParameters = SessionCreationParameters(
            cwd = "/workspace/project",
            mcpServers = listOf(
                McpServer.Stdio(name = "stdio", command = "cagent", args = listOf("serve"), env = emptyList()),
                McpServer.Http(name = "http", url = "https://example.test/mcp", headers = emptyList()),
                McpServer.Sse(name = "sse", url = "https://example.test/events", headers = emptyList())
            )
        )

        val session = runner.createAgentSupport().createSession(sessionParameters)
        val createdSession = bridge.createdSessions.single()

        assertTrue(session.sessionId.value.startsWith("session-"))
        assertNotEquals("default-session", session.sessionId.value)
        assertEquals(createdSession.sessionId, session.sessionId.value)
        assertEquals("/workspace/project", createdSession.cwd)
        assertEquals(
            mapOf(
                "stdio" to "cagent",
                "http" to "https://example.test/mcp",
                "sse" to "https://example.test/events"
            ),
            createdSession.mcpServers
        )
    }

    @Test
    fun `loadSession reuses the requested session id and forwards session context`() = runBlocking {
        val bridge = RecordingBridge()
        val runner = AcpAgentRunner(bridge)
        val sessionParameters = SessionCreationParameters(
            cwd = "/workspace/load",
            mcpServers = listOf(McpServer.Stdio(name = "repo-tools", command = "repo-tools", args = emptyList(), env = emptyList()))
        )

        val session = runner.createAgentSupport().loadSession(SessionId("existing-session"), sessionParameters)
        val createdSession = bridge.createdSessions.single()

        assertEquals("existing-session", session.sessionId.value)
        assertEquals("existing-session", createdSession.sessionId)
        assertEquals("/workspace/load", createdSession.cwd)
        assertEquals(mapOf("repo-tools" to "repo-tools"), createdSession.mcpServers)
    }

    @Test
    fun `bridge session converts streamed tokens to ACP events and delegates cancellation`() = runBlocking {
        val resourceLink = ContentBlock.ResourceLink(name = "README", uri = "file:///README.md")
        val session = RecordingSession("session-123", streamedTokens = listOf("chunk-1", "chunk-2"))
        val runner = AcpAgentRunner(RecordingBridge(session))

        val events = runner.createBridgeAgentSession(session)
            .prompt(listOf(ContentBlock.Text("Hello"), ContentBlock.Text("World"), resourceLink), null)
            .toList()

        assertEquals("Hello\nWorld", session.lastPromptText)
        assertEquals(listOf(resourceLink), session.lastResourceLinks)
        assertEquals(
            listOf("chunk-1", "chunk-2"),
            events.filterIsInstance<Event.SessionUpdateEvent>()
                .mapNotNull { (it.update as? SessionUpdate.AgentMessageChunk)?.content as? ContentBlock.Text }
                .map { it.text }
        )
        assertEquals(
            StopReason.END_TURN,
            events.filterIsInstance<Event.PromptResponseEvent>().single().response.stopReason
        )

        runner.createBridgeAgentSession(session).cancel()
        assertEquals(1, session.cancelCount.get())
    }

    @Test
    fun `prompt flow buffer capacity uses the bounded default`() {
        assertEquals(Channel.BUFFERED, AcpAgentRunner.PROMPT_FLOW_BUFFER_CAPACITY)
    }

    private data class CreatedSession(
        val sessionId: String,
        val cwd: String,
        val mcpServers: Map<String, String>
    )

    private class RecordingBridge(
        private val fixedSession: RecordingSession? = null
    ) : AcpAgentSupportBridge {
        val createdSessions = mutableListOf<CreatedSession>()

        override fun getAgentInfo(): AcpAgentSupportBridge.AgentInfoBridge =
            AcpAgentSupportBridge.AgentInfoBridge("BridgeAgent", "9.9.9")

        override fun createSession(sessionId: String, cwd: String, mcpServers: Map<String, String>): AcpAgentSupportBridge.AcpSessionBridge {
            createdSessions += CreatedSession(sessionId, cwd, mcpServers)
            return fixedSession ?: RecordingSession(sessionId)
        }
    }

    private class RecordingSession(
        private val id: String,
        private val streamedTokens: List<String> = emptyList()
    ) : AcpAgentSupportBridge.AcpSessionBridge {
        var lastPromptText: String? = null
        var lastResourceLinks: List<ContentBlock.ResourceLink> = emptyList()
        val cancelCount = AtomicInteger()
        private val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)

        override fun getSessionId(): String = id

        override fun cancelledFlag(): java.util.concurrent.atomic.AtomicBoolean = cancelled

        override fun processPrompt(
            promptText: String,
            resourceLinks: List<ContentBlock.ResourceLink>
        ): CompletableFuture<String> = CompletableFuture.completedFuture(promptText)

        override fun streamPrompt(
            promptText: String,
            promtResourceLinks: List<ContentBlock.ResourceLink>,
            consumer: AcpAgentSupportBridge.TokenConsumer
        ) {
            lastPromptText = promptText
            lastResourceLinks = promtResourceLinks
            streamedTokens.forEach(consumer::onNext)
            consumer.onComplete()
        }

        override fun cancel() {
            cancelCount.incrementAndGet()
        }
    }
}

