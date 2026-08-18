package io.kudos.ability.comm.websocket.spring

import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketExtension
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress
import java.net.URI
import java.security.Principal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory [WebSocketSession] for unit tests: records what was sent and how it was closed, and can
 * be told to fail a send.
 *
 * A fake rather than a mock because the assertions here are about *sequences* — which messages
 * arrived, in what order, and whether a close followed — which a fake expresses directly and a mock
 * only through verification order stubs.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class FakeWebSocketSession(
    private val sessionId: String = "s1",
    private val sessionUri: URI? = URI("ws://localhost/ws"),
    private val headers: HttpHeaders = HttpHeaders(),
) : WebSocketSession {

    /** Every message handed to [sendMessage], in order. */
    val sent = CopyOnWriteArrayList<WebSocketMessage<*>>()

    /** Text payloads only, for the common assertion. */
    val sentText: List<String> get() = sent.filterIsInstance<org.springframework.web.socket.TextMessage>().map { it.payload }

    @Volatile
    var closedWith: CloseStatus? = null
        private set

    /** When non-null, [sendMessage] throws it instead of recording. */
    @Volatile
    var failSendWith: Throwable? = null

    @Volatile
    private var open = true

    private val attrs = ConcurrentHashMap<String, Any>()

    override fun getId(): String = sessionId

    override fun getUri(): URI? = sessionUri

    override fun getHandshakeHeaders(): HttpHeaders = headers

    override fun getAttributes(): MutableMap<String, Any> = attrs

    override fun getPrincipal(): Principal? = null

    override fun getLocalAddress(): InetSocketAddress? = null

    override fun getRemoteAddress(): InetSocketAddress? = null

    override fun getAcceptedProtocol(): String? = null

    override fun setTextMessageSizeLimit(messageSizeLimit: Int) = Unit

    override fun getTextMessageSizeLimit(): Int = 0

    override fun setBinaryMessageSizeLimit(messageSizeLimit: Int) = Unit

    override fun getBinaryMessageSizeLimit(): Int = 0

    override fun getExtensions(): List<WebSocketExtension> = emptyList()

    override fun sendMessage(message: WebSocketMessage<*>) {
        failSendWith?.let { throw it }
        sent += message
    }

    override fun isOpen(): Boolean = open

    override fun close() = close(CloseStatus.NORMAL)

    override fun close(status: CloseStatus) {
        closedWith = status
        open = false
    }
}
