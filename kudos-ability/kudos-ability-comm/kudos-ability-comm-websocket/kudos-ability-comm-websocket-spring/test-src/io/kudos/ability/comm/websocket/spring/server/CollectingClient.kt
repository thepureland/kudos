package io.kudos.ability.comm.websocket.spring.server

import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import org.springframework.web.socket.handler.AbstractWebSocketHandler
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull

/**
 * Real WebSocket client for the end-to-end suite: a genuine handshake against the running container,
 * not a mock.
 *
 * `StandardWebSocketClient` drives the Jakarta WebSocket client that ships with the embedded Tomcat,
 * so a rejected upgrade surfaces the same way it would for a browser — as a failed connect carrying an
 * HTTP status, rather than as a session that opens and then closes.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class CollectingClient : AbstractWebSocketHandler() {

    val texts = LinkedBlockingQueue<String>()
    val binaries = LinkedBlockingQueue<ByteArray>()
    private val closes = LinkedBlockingQueue<CloseStatus>()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        texts += message.payload
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val buffer: ByteBuffer = message.payload.duplicate()
        binaries += ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        closes += status
    }

    /** Waits for one text message; fails the test rather than returning null on timeout. */
    fun awaitText(timeoutSeconds: Long = 5): String =
        assertNotNull(texts.poll(timeoutSeconds, TimeUnit.SECONDS), "No text message arrived within ${timeoutSeconds}s")

    fun awaitBinary(timeoutSeconds: Long = 5): ByteArray =
        assertNotNull(binaries.poll(timeoutSeconds, TimeUnit.SECONDS), "No binary message arrived within ${timeoutSeconds}s")

    /** Waits for the server to close the connection. */
    fun awaitClose(timeoutSeconds: Long = 5): CloseStatus =
        assertNotNull(closes.poll(timeoutSeconds, TimeUnit.SECONDS), "The connection was not closed within ${timeoutSeconds}s")

    companion object {

        /**
         * The Jakarta 8 KB default applies to the **client** container too, so a client left at the
         * default cannot receive the oversized message the server-side buffer test sends back — the test
         * would fail on the client's limit while proving nothing about the server's. Raised once and
         * shared, because `ContainerProvider.getWebSocketContainer()` builds a new container (with its own
         * threads) on every call.
         */
        private val container by lazy {
            jakarta.websocket.ContainerProvider.getWebSocketContainer().apply {
                defaultMaxTextMessageBufferSize = 1024 * 1024
                defaultMaxBinaryMessageBufferSize = 1024 * 1024
            }
        }

        private fun newClient() = StandardWebSocketClient(container)

        /*
         * Every URL below uses the literal 127.0.0.1 rather than "localhost".
         *
         * SockJS ties a session to the client's exact address, so a dual-stack host resolving "localhost"
         * to 127.0.0.1 for one connection and ::1 for the next is enough to make the server reject the
         * second one. Pinning the literal keeps every connection on one address family.
         */

        /**
         * Opens a connection and returns the session plus this collector.
         *
         * @param origin sent as the `Origin` header when non-null — the header a browser always sends and
         *   the one Spring's default same-origin check reads
         */
        fun connect(port: Int, path: String, origin: String? = null): Pair<WebSocketSession, CollectingClient> {
            val client = CollectingClient()
            val headers = WebSocketHttpHeaders().apply { origin?.let { this.origin = it } }
            val session = newClient()
                .execute(client, headers, URI("ws://127.0.0.1:$port$path"))
                .get(10, TimeUnit.SECONDS)
            session.textMessageSizeLimit = 1024 * 1024
            session.binaryMessageSizeLimit = 1024 * 1024
            return session to client
        }

        /**
         * Connects through SockJS rather than natively.
         *
         * Uses the real `SockJsClient`, so this exercises the extra URL surface `withSockJS()` mounts —
         * the `/info` negotiation and the `/{server}/{session}/websocket` transport URL — not just the
         * plain endpoint under a different path. Note the `http://` scheme: SockJS negotiates over HTTP
         * before choosing a transport.
         */
        fun connectViaSockJs(port: Int, path: String): Pair<WebSocketSession, CollectingClient> {
            val client = CollectingClient()
            val sockJs = SockJsClient(listOf(WebSocketTransport(newClient())))
            val session = sockJs
                .execute(client, "http://127.0.0.1:$port$path")
                .get(15, TimeUnit.SECONDS)
            return session to client
        }


        /** Attempts a connection and returns the failure, for the cases where the upgrade must be refused. */
        fun connectExpectingFailure(port: Int, path: String, origin: String? = null): Throwable {
            val headers = WebSocketHttpHeaders().apply { origin?.let { this.origin = it } }
            return runCatching {
                newClient()
                    .execute(CollectingClient(), headers, URI("ws://127.0.0.1:$port$path"))
                    .get(10, TimeUnit.SECONDS)
            }.exceptionOrNull() ?: error("The handshake to $path succeeded, but it was expected to be refused")
        }
    }
}
