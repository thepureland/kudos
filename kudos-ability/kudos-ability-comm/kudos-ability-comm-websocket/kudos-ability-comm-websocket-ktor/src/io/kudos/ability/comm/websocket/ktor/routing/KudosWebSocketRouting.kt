package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.connect.admit
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.kudos.ability.comm.websocket.ktor.session.toKtor
import io.kudos.base.logger.LogFactory
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Private logger-category anchor: this file only contains top-level functions, so a dedicated object keeps log events attributed to the routing component instead of an unrelated class. */
private object KudosWebSocketRouting

private val log = LogFactory.getLog(KudosWebSocketRouting::class)

/**
 * Upper bound on a single reassembled message. The Ktor `WebSockets` plugin installed by
 * `kudos-ability-web-ktor` sets `maxFrameSize = Long.MAX_VALUE`, so without a limit here a client
 * could stream unbounded fragments into a server-side buffer.
 */
const val DEFAULT_MAX_MESSAGE_SIZE: Long = 10L * 1024 * 1024

/**
 * Ktor `Route` extension that mounts an [IKudosWebSocketHandler] on the WebSocket route at
 * [path]. Handles the full "authenticate → register → loop → unregister" template:
 *
 * ```kotlin
 * routing {
 *     kudosWebSocket("/chat", registry, ChatHandler()) { raw ->
 *         val userId = raw.call.principal<JWTPrincipal>()?.payload?.subject
 *             ?: return@kudosWebSocket null   // rejected: never registered, never broadcast to
 *         KudosWebSocketSession(
 *             raw = raw,
 *             userId = userId,
 *             tenantId = raw.call.request.headers["X-Tenant-Id"],
 *         )
 *     }
 * }
 * ```
 *
 * Design notes:
 *  - **[sessionFactory] is the identification seam.** It is `suspend`, so it can hit a database or
 *    Redis to validate a token, and it may return `null` to reject the connection. Rejecting there
 *    matters: the session is registered immediately afterwards, so a connection that is admitted
 *    first and closed later inside `onConnect` is, for that window, a member of its claimed
 *    `tenantId` and receives that tenant's broadcasts.
 *  - **[connectInterceptors] are the admission seam**, running after the factory and before
 *    registration — the window where identity is known but the connection is not yet reachable by
 *    broadcasts. Unlike the single factory lambda they compose, so per-user device caps, per-tenant
 *    quotas and the like stay independent of each other and of session construction. The first
 *    rejection wins; see [IWebSocketConnectInterceptor].
 *  - **Per-message interception** is not a separate mechanism: wrap [handler] in a
 *    [io.kudos.ability.comm.websocket.common.handler.WebSocketHandlerDecorator]
 *    (see `wrappedBy`), which can run code around the delegate and short-circuit it.
 *  - **Handshake-level concerns stay in Ktor.** This is an ordinary `Route` extension, so
 *    `authenticate("jwt") { kudosWebSocket(...) }` composes with Ktor's own `Authentication` plugin;
 *    this module does not reimplement that layer.
 *  - **Connection exceptions** are caught and the cause is propagated via
 *    [IKudosWebSocketHandler.onDisconnect].
 *  - **Teardown always runs** — unregister included — regardless of how the loop ended, so the
 *    registry cannot fill up with dead sessions. It runs under [NonCancellable] so that suspending
 *    cleanup still completes when the route coroutine is cancelled during a graceful shutdown;
 *    otherwise `onDisconnect` would abort at its first suspension point and business cleanup
 *    (clearing presence keys, flushing audit rows) would be silently skipped exactly when the whole
 *    fleet is restarting.
 *  - **Cancellation is re-thrown** after teardown, so the surrounding structured-concurrency scope
 *    still sees the route as cancelled rather than as a clean return.
 *  - **Makes no assumption** about the wire protocol (JSON / Protobuf / etc.) — this extension
 *    only reassembles frames into whole messages and passes them to the handler; higher-level
 *    serialization is the handler's business (see
 *    [io.kudos.ability.comm.websocket.common.handler.TypedWebSocketHandler]).
 *
 * Multiple `kudosWebSocket` routes can share the same [KudosWebSocketRegistry] — the typical
 * pattern is a process-level singleton on the business side.
 *
 * @param maxMessageSize reject and close connections whose reassembled message exceeds this size
 * @param connectInterceptors admission checks run in order after [sessionFactory], before registration
 * @param rejectCloseReason close reason sent when [sessionFactory] rejects the connection
 * @author K
 * @since 1.0.0
 */
fun Route.kudosWebSocket(
    path: String,
    registry: KudosWebSocketRegistry,
    handler: IKudosWebSocketHandler,
    maxMessageSize: Long = DEFAULT_MAX_MESSAGE_SIZE,
    connectInterceptors: List<IWebSocketConnectInterceptor> = emptyList(),
    rejectCloseReason: WebSocketCloseReason = WebSocketCloseReason.REJECTED,
    sessionFactory: suspend (DefaultWebSocketServerSession) -> KudosWebSocketSession? = { KudosWebSocketSession(it) },
) {
    webSocket(path) {
        val session = try {
            sessionFactory(this)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            log.warn("WebSocket sessionFactory threw, rejecting connection path={0} cause={1}", path, t.message)
            null
        }
        if (session == null) {
            log.debug("WebSocket connection rejected by sessionFactory path={0}", path)
            runCatching { close(rejectCloseReason.toKtor()) }
            return@webSocket
        }

        val rejection = connectInterceptors.admit(session, path)
        if (rejection != null) {
            runCatching { close(rejection.reason.toKtor()) }
            return@webSocket
        }

        registry.register(session)
        var cause: Throwable? = null
        try {
            handler.onConnect(session)
            dispatchFrames(incoming, session, handler, maxMessageSize)
        } catch (t: Throwable) {
            cause = t
            if (t is CancellationException) {
                log.debug("WebSocket session cancelled sessionId={0} path={1}", session.sessionId, path)
            } else {
                log.warn("WebSocket handling exception sessionId={0} path={1} cause={2}",
                    session.sessionId, path, t.message)
            }
        } finally {
            withContext(NonCancellable) {
                runCatching { handler.onDisconnect(session, cause) }
                    .onFailure { log.warn("onDisconnect threw an exception sessionId={0} cause={1}", session.sessionId, it.message) }
                registry.unregister(session)
                runCatching { close(WebSocketCloseReason.NORMAL.toKtor()) }
            }
        }
        if (cause is CancellationException) throw cause
    }
}

/**
 * Frame-dispatch loop body of [kudosWebSocket]. Turns a stream of wire frames into whole
 * application messages:
 *
 *  - **Fragmented messages are reassembled.** Ktor does not do this for you: its `WebSocketReader`
 *    emits every fragment as its own [Frame] carrying `fin = false`, and re-labels continuation
 *    frames with the opcode of the message they belong to. Dispatching each fragment directly would
 *    hand the handler partial messages, and would corrupt any multi-byte UTF-8 character that
 *    happens to straddle a fragment boundary. (The Spring engine module needs no equivalent: the
 *    Servlet container reassembles before invoking the handler.)
 *  - **Control frames pass through mid-message.** Ping / Pong may legally interleave a fragmented
 *    message, so they are skipped without disturbing the buffer. The Ktor plugin already answers
 *    pings, so the business side never needs to see them.
 *  - **A Close frame ends the loop** immediately.
 *  - **[maxMessageSize] is enforced** on both single frames and reassembled messages; exceeding it
 *    closes the connection with `TOO_BIG` rather than growing the buffer further.
 *
 * Extracted as `internal` (rather than kept inline) purely for testability: a real engine makes the
 * Close / Ping / Pong and fragmentation branches awkward to reach, so unit tests drive them
 * directly with a plain channel.
 */
internal suspend fun dispatchFrames(
    incoming: ReceiveChannel<Frame>,
    session: KudosWebSocketSessionRef,
    handler: IKudosWebSocketHandler,
    maxMessageSize: Long = DEFAULT_MAX_MESSAGE_SIZE,
) {
    var pending: ByteArrayOutputStream? = null
    var pendingType: FrameType? = null

    for (frame in incoming) {
        when (frame) {
            is Frame.Close -> break
            // Control frames may interleave a fragmented message: skip without touching the buffer.
            is Frame.Ping, is Frame.Pong -> continue
            is Frame.Text, is Frame.Binary -> {
                val buffered = pending?.size() ?: 0
                if (buffered + frame.data.size > maxMessageSize) {
                    log.warn("WebSocket message exceeds maxMessageSize={0} sessionId={1}; closing", maxMessageSize, session.sessionId)
                    runCatching { session.close(WebSocketCloseReason(WebSocketCloseReason.Codes.TOO_BIG, "Message too big")) }
                    break
                }
                if (frame.fin && pending == null) {
                    dispatch(frame.frameType, frame.data, session, handler) // fast path: whole message in one frame
                    continue
                }
                val buffer = pending ?: ByteArrayOutputStream().also {
                    pending = it
                    pendingType = frame.frameType
                }
                buffer.write(frame.data)
                if (frame.fin) {
                    dispatch(pendingType ?: frame.frameType, buffer.toByteArray(), session, handler)
                    pending = null
                    pendingType = null
                }
            }
        }
    }
}

/** Routes one complete message to the matching handler hook. */
private suspend fun dispatch(
    type: FrameType,
    data: ByteArray,
    session: KudosWebSocketSessionRef,
    handler: IKudosWebSocketHandler,
) {
    when (type) {
        FrameType.TEXT -> handler.onText(session, data.toString(Charsets.UTF_8))
        FrameType.BINARY -> handler.onBinary(session, data)
        else -> {} // control frames never reach here
    }
}
