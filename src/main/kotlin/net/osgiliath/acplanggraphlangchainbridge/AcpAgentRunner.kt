package net.osgiliath.acplanggraphlangchainbridge

import com.agentclientprotocol.agent.Agent
import com.agentclientprotocol.agent.AgentInfo
import com.agentclientprotocol.agent.AgentSession
import com.agentclientprotocol.agent.AgentSupport
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.*
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge
import net.osgiliath.acplanggraphlangchainbridge.acp.InAcpAdapter
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.stream.Collectors

/**
 * ACP Agent Runner - Uses official JetBrains ACP Kotlin SDK
 * Bridges the SDK with Java implementation.
 */
@Component
class AcpAgentRunner(
    private val agentSupportBridge: InAcpAdapter
) : CommandLineRunner {

    companion object {
        internal const val PROMPT_FLOW_BUFFER_CAPACITY: Int = Channel.BUFFERED
    }

    private val log = LoggerFactory.getLogger(AcpAgentRunner::class.java)

    override fun run(vararg args: String?) {
        log.info("Starting ACP Agent Runner using official SDK")

        runBlocking {
            val transport = StdioTransport(
                parentScope = this,
                ioDispatcher = Dispatchers.IO,
                input = System.`in`.asSource().buffered(),
                output = System.out.asSink().buffered()
            )
            val protocol = Protocol(this, transport)

            val agentSupport = createAgentSupport()

            Agent(protocol, agentSupport)
            protocol.start()
            log.info("Agent started, waiting for requests on stdin...")

            // Keep the transport alive until it's closed
            val deferred = CompletableDeferred<Unit>()
            transport.onClose { deferred.complete(Unit) }
            deferred.await()
        }
    }

    internal fun createAgentSupport(): AgentSupport = object : AgentSupport {
        override suspend fun initialize(clientInfo: com.agentclientprotocol.client.ClientInfo): AgentInfo {
            val agentInfo = agentSupportBridge.agentInfo
            return AgentInfo(
                protocolVersion = LATEST_PROTOCOL_VERSION,
                capabilities = AgentCapabilities(loadSession = true),
                implementation = Implementation(agentInfo.name, agentInfo.version)
            )
        }

        override suspend fun createSession(sessionParameters: SessionCreationParameters): AgentSession {
            val javaSession = createJavaSession(generateSessionId(), sessionParameters)
            return createBridgeAgentSession(javaSession)
        }

        override suspend fun loadSession(
            sessionId: SessionId,
            sessionParameters: SessionCreationParameters
        ): AgentSession {
            val javaSession = createJavaSession(sessionId.value, sessionParameters)
            return createBridgeAgentSession(javaSession)
        }
    }

    internal fun createBridgeAgentSession(acpSession: AcpAgentSupportBridge.AcpSessionBridge): AgentSession =
        BridgeAgentSession(acpSession)

    private fun createJavaSession(
        sessionId: String,
        sessionParameters: SessionCreationParameters
    ): AcpAgentSupportBridge.AcpSessionBridge {
        log.info("Creating Java session $sessionId")
        log.debug("Session parameters: cwd=${sessionParameters.cwd}, mcpServers=${sessionParameters.mcpServers}")
        return agentSupportBridge.createSession(
            sessionId,
            sessionParameters.cwd,
            sessionParameters.mcpServers.associate { server ->
                server.name to when (server) {
                    is McpServer.Stdio -> server.command
                    is McpServer.Http -> server.url
                    is McpServer.Sse -> server.url
                }
            }
        )
    }

    private fun generateSessionId(): String = "session-${UUID.randomUUID()}"

    private inner class BridgeAgentSession(
        private val acpSession: AcpAgentSupportBridge.AcpSessionBridge
    ) : AgentSession {
        override val sessionId: SessionId = SessionId(acpSession.sessionId)

        override suspend fun prompt(
            content: List<ContentBlock>,
            _meta: kotlinx.serialization.json.JsonElement?
        ): Flow<Event> = callbackFlow {
            log.trace("Agent prompt started")
            log.debug("Content blocks received: ${content.size} for session ${acpSession.sessionId}")
            if (log.isDebugEnabled) {
                val iterator = content.stream().collect(Collectors.toList()).iterator()
                while (iterator.hasNext()) {
                    log.debug("Content name: ${iterator.next()}")
                }
                log.debug("Meta information: ${_meta}")
            }
            val promptText = content.filterIsInstance<ContentBlock.Text>()
                .joinToString("\n") { it.text }
            val promtResourceLinks = content.filterIsInstance<ContentBlock.ResourceLink>()
            log.debug("Prompt resource links received: {}", promtResourceLinks)
            log.info("Processing streaming prompt for session ${acpSession.sessionId}")

            // Launch the blocking streamPrompt call on Dispatchers.IO so it
            // does NOT monopolise the single-threaded runBlocking dispatcher.
            // Without this, the flow collector cannot run concurrently and all
            // tokens accumulate in the buffer, producing a single-block response.
            launch(Dispatchers.IO) {
                acpSession.streamPrompt(promptText, promtResourceLinks, object : AcpAgentSupportBridge.TokenConsumer {
                    override fun onNext(token: String) {
                        trySend(Event.SessionUpdateEvent(SessionUpdate.AgentMessageChunk(ContentBlock.Text(token))))
                            .exceptionOrNull()?.let { e -> log.warn("Failed to send streaming token", e) }
                    }

                    override fun onComplete() {
                        trySend(Event.PromptResponseEvent(PromptResponse(stopReason = StopReason.END_TURN)))
                            .exceptionOrNull()?.let { e -> log.warn("Failed to send completion event", e) }
                        close()
                    }

                    override fun onError(error: Throwable) {
                        log.error("Error during streaming prompt", error)
                        trySend(Event.SessionUpdateEvent(SessionUpdate.AgentMessageChunk(ContentBlock.Text("Error: ${error.message}"))))
                        trySend(Event.PromptResponseEvent(PromptResponse(stopReason = StopReason.END_TURN)))
                        close(error)
                    }
                })
            }

            awaitClose {
                log.debug("Streaming prompt flow closed for session ${acpSession.sessionId}")
            }
        }.buffer(PROMPT_FLOW_BUFFER_CAPACITY)

        override suspend fun cancel() {
            acpSession.cancel()
        }
    }
}
