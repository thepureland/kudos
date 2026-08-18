package io.kudos.ability.comm.websocket.spring.session

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Kudos business-layer wrapper around a Spring [WebSocketSession].
 *
 * The Spring-side implementation of [KudosWebSocketSessionRef], mirroring
 * `io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession` so that the registry,
 * broadcaster, handler SPI and admission interceptors in `kudos-ability-comm-websocket-common` work
 * identically on both engines.
 *
 * ## Two things this wrapper exists to fix
 *
 * **1. Blocking sends must not run on a coroutine's thread.** `WebSocketSession.sendMessage` is a
 * blocking call — it writes to the socket and, on a slow client, waits. Every hook in
 * [io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler] is `suspend`, and the
 * broadcaster fans out from coroutines, so calling it directly would pin whatever dispatcher thread
 * happens to be running. Every send here is therefore offloaded to [Dispatchers.IO].
 *
 * **2. `WebSocketSession` is not safe for concurrent use.** Two threads calling `sendMessage` on the
 * same session throw `IllegalStateException: The remote endpoint was in state [TEXT_FULL_WRITING]`.
 * That is not a theoretical race: a broadcast reaches many sessions concurrently, and a business
 * push racing a reply on the same connection is the normal case. This class does **not** add locking
 * of its own — instead the adapter wraps every session in Spring's own
 * `ConcurrentWebSocketSessionDecorator` before constructing this class, which serializes sends *and*
 * bounds the buffer a slow client can accumulate. See
 * `io.kudos.ability.comm.websocket.spring.handler.KudosSpringWebSocketHandler` for the wrapping and
 * the limits.
 *
 * Consequently [raw] is normally that decorator, not the container's session object. Code that needs
 * the underlying one (to read a container-specific attribute, say) should call
 * `org.springframework.web.socket.handler.WebSocketSessionDecorator.unwrap(raw)` rather than assume
 * a concrete type.
 *
 * [sessionId] defaults to the container's own [WebSocketSession.getId] rather than a freshly
 * generated UUID: it is already unique per connection, and keeping it means a session id in a kudos
 * log lines up with the same id in the container's logs.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class SpringWebSocketSession(
    val raw: WebSocketSession,
    override val sessionId: String = raw.id,
    override val userId: String? = null,
    override val tenantId: String? = null,
) : KudosWebSocketSessionRef {

    override val attributes: MutableMap<String, Any> = ConcurrentHashMap()

    /** Sends a text message. Blocking I/O is offloaded to [Dispatchers.IO]. */
    override suspend fun sendText(text: String) {
        withContext(Dispatchers.IO) { raw.sendMessage(TextMessage(text)) }
    }

    /** Sends a binary message. Blocking I/O is offloaded to [Dispatchers.IO]. */
    override suspend fun sendBinary(bytes: ByteArray) {
        withContext(Dispatchers.IO) { raw.sendMessage(BinaryMessage(bytes)) }
    }

    /**
     * Closes the connection, mapping the engine-neutral [WebSocketCloseReason] onto Spring's
     * [CloseStatus].
     *
     * A close on an already-closed session is skipped rather than attempted: the container fires
     * `afterConnectionClosed` before the handler's own teardown runs, so the normal shutdown path
     * would otherwise always attempt a redundant close and log an exception for it.
     *
     * Note that codes 1005 and 1006 cannot be sent — see [WebSocketCloseReason.Codes]. Passing one
     * makes the container reject the frame.
     */
    override suspend fun close(reason: WebSocketCloseReason) {
        if (!raw.isOpen) return
        withContext(Dispatchers.IO) { raw.close(CloseStatus(reason.code.toInt(), reason.message)) }
    }
}
