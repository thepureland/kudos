package io.kudos.ability.comm.websocket.spring.handler

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.connect.admit
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import io.kudos.ability.comm.websocket.common.session.WebSocketPayload
import io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession
import io.kudos.base.logger.LogFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.DisposableBean
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.util.concurrent.ConcurrentHashMap

/**
 * Spring [WebSocketHandler] that drives an [IKudosWebSocketHandler].
 *
 * The Spring counterpart of Ktor's `Route.kudosWebSocket`, running the same lifecycle template —
 * **identify → admit → register → deliver → unregister** — so business code written against
 * [IKudosWebSocketHandler] behaves identically on either engine.
 *
 * ## The problem this class actually solves
 *
 * Spring's [WebSocketHandler] callbacks are blocking, one-shot methods invoked on a container
 * thread. [IKudosWebSocketHandler]'s hooks are `suspend`. Bridging them naively goes wrong in three
 * ways, and all three are the kind of bug that only appears under load:
 *
 *  - **`runBlocking` per message** ties up a container thread for the whole of business processing.
 *    That is Tomcat's request-handling pool, shared with every HTTP endpoint in the application.
 *  - **`scope.launch` per message** frees the thread but *destroys message ordering*: two chat
 *    messages a millisecond apart get their own coroutines and finish in whatever order the
 *    scheduler picks. Users notice immediately, and it is untraceable after the fact.
 *  - **Either, without a bound**, lets a client that sends faster than the server processes queue
 *    unbounded work in the heap.
 *
 * So each connection gets **one bounded [Channel] drained by exactly one coroutine**. Ordering is
 * preserved because there is a single consumer; the container thread is released as soon as the
 * message is queued; and the bound is what makes overflow a decision ([Options.inboundOverflow])
 * rather than an out-of-memory error.
 *
 * ## Backpressure is the default, and it is not free
 *
 * With [InboundOverflowPolicy.BACKPRESSURE] a full queue blocks the container thread inside
 * [handleMessage] until the pump catches up. That is deliberate: blocking there stops the container
 * reading that connection's socket, TCP flow control kicks in, and the client is throttled at the
 * transport layer — which is the only place a WebSocket can be throttled without losing data.
 *
 * The cost is one occupied container thread per backed-up connection. On Java 21+ with
 * `spring.threads.virtual.enabled=true` that is nearly free. On a platform-thread pool it is not, so
 * [InboundOverflowPolicy.DROP_LATEST] and [InboundOverflowPolicy.CLOSE] are offered for deployments
 * that would rather shed load than hold threads. Note that the fast path never blocks at all —
 * `trySend` succeeds whenever the queue has room, which is the normal case.
 *
 * ## Concurrent sends
 *
 * Every session is wrapped in Spring's [ConcurrentWebSocketSessionDecorator] before the business
 * session is built. Without it, a broadcast and a reply landing on the same connection at the same
 * moment throw `IllegalStateException: The remote endpoint was in state [TEXT_FULL_WRITING]` — and
 * a broadcaster fanning out to many sessions makes that a routine occurrence, not a rare race. The
 * decorator also bounds how many bytes a client that has stopped reading may accumulate
 * ([Options.sendBufferSizeLimitBytes]) and terminates it past the limit, which is the Spring-side
 * equivalent of the send timeout `WebSocketBroadcaster` applies.
 *
 * ## Fragmentation
 *
 * [supportsPartialMessages] is `false`, so the servlet container reassembles fragmented messages
 * before calling [handleMessage] — this class needs none of the frame-reassembly logic Ktor's
 * routing extension carries. The size ceiling therefore lives on the container instead; see
 * `SpringWebSocketAutoConfiguration`'s `ServletServerContainerFactoryBean`.
 *
 * @param registry the process-level session registry, shared with the broadcaster
 * @param handler business SPI implementation
 * @param sessionFactory identification seam; returning `null` rejects the connection
 * @param connectInterceptors admission checks run after [sessionFactory], before registration
 * @param options queue / send / shutdown limits
 * @param dispatcher where handler hooks run. Defaults to [Dispatchers.IO] rather than
 *   [Dispatchers.Default] because business code in a Spring MVC application is overwhelmingly
 *   blocking (JDBC, `RestClient`), and blocking on the CPU-bound dispatcher starves every other
 *   coroutine in the process. Pass [Dispatchers.Default] if the handlers really are non-blocking.
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class KudosSpringWebSocketHandler(
    private val registry: KudosWebSocketRegistry,
    private val handler: IKudosWebSocketHandler,
    private val sessionFactory: ISpringWebSocketSessionFactory = ISpringWebSocketSessionFactory { SpringWebSocketSession(it) },
    private val connectInterceptors: List<IWebSocketConnectInterceptor> = emptyList(),
    private val options: Options = Options(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WebSocketHandler, DisposableBean {

    private val log = LogFactory.getLog(this::class)

    /** One supervisor for every pump, so a coroutine that dies does not take its siblings with it. */
    private val supervisor = SupervisorJob()

    private val scope = CoroutineScope(supervisor + dispatcher)

    /** Keyed by the container's session id, which is what every callback below is handed. */
    private val pumps = ConcurrentHashMap<String, Pump>()

    /** Number of connections this handler is currently pumping. Exposed for health checks and tests. */
    val activeConnections: Int get() = pumps.size

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val decorated = ConcurrentWebSocketSessionDecorator(
            session,
            options.sendTimeLimitMillis,
            options.sendBufferSizeLimitBytes,
            options.sendOverflowStrategy,
        )
        val pump = Pump(Channel(options.inboundBufferCapacity))
        // Published before the coroutine starts: handleMessage may fire the instant this method
        // returns, and a message arriving before the map entry exists would be dropped silently.
        pumps[session.id] = pump
        scope.launch { pump(decorated, pump) }
    }

    /**
     * Queues one complete message for the session's pump.
     *
     * Ping and Pong are dropped: the container answers Ping itself, and a Pong is a liveness signal
     * with nothing in it for business code. Surfacing them would make every handler implement the
     * same empty branch.
     */
    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        val payload = when (message) {
            is TextMessage -> WebSocketPayload.Text(message.payload)
            is BinaryMessage -> WebSocketPayload.Binary(message.payload.toByteArray())
            else -> return
        }
        val pump = pumps[session.id] ?: return
        deliver(pump, payload, session)
    }

    /**
     * Records the transport failure so the pump can report it through
     * [IKudosWebSocketHandler.onDisconnect].
     *
     * The connection is not closed here — the container follows a transport error with
     * [afterConnectionClosed], and closing early would race that.
     */
    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        pumps[session.id]?.transportError = exception
        log.warn("WebSocket transport error sessionId={0} cause={1}", session.id, exception.message)
    }

    /**
     * Ends the pump by closing its queue.
     *
     * Closing rather than cancelling is what lets messages already queued be processed before
     * teardown: a client that sends a final message and immediately disconnects should still have it
     * handled. The pump's own `finally` then runs `onDisconnect` and unregisters.
     */
    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        val pump = pumps.remove(session.id) ?: return
        pump.closeStatus = closeStatus
        pump.inbound.close()
    }

    /**
     * `false`, so the servlet container reassembles fragmented messages and every
     * [IKudosWebSocketHandler] hook receives one whole application message.
     *
     * Returning `true` would hand business code half a message — and split any multi-byte UTF-8
     * character that happened to straddle the boundary.
     */
    override fun supportsPartialMessages(): Boolean = false

    /**
     * Graceful shutdown: stop accepting into the queues, let in-flight messages and every
     * `onDisconnect` finish, then cancel whatever is left.
     *
     * This runs because Spring calls [DisposableBean.destroy] on context close. Without it, the
     * process would exit with pump coroutines mid-message and business cleanup — clearing presence
     * keys, flushing audit rows — silently skipped on every rolling restart, which is exactly when
     * the whole fleet does it at once.
     */
    override fun destroy() {
        val draining = pumps.values.toList()
        pumps.clear()
        draining.forEach { runCatching { it.inbound.close() } }
        runBlocking {
            val drained = withTimeoutOrNull(options.shutdownTimeoutMillis) {
                supervisor.children.toList().joinAll()
            }
            if (drained == null) {
                log.warn("WebSocket pumps did not finish within {0} ms; cancelling {1} remaining connection(s)",
                    options.shutdownTimeoutMillis, draining.size)
            }
            supervisor.cancelAndJoin()
        }
    }

    /** Queues [payload], applying [Options.inboundOverflow] when the queue is full. */
    private fun deliver(pump: Pump, payload: WebSocketPayload, session: WebSocketSession) {
        if (pump.inbound.trySend(payload).isSuccess) return
        when (options.inboundOverflow) {
            InboundOverflowPolicy.BACKPRESSURE ->
                // Blocks this container thread until the pump drains, which stops the container
                // reading the socket and lets TCP throttle the client. A send on a channel closed
                // concurrently by afterConnectionClosed is not an error — the connection is gone.
                runCatching { runBlocking { pump.inbound.send(payload) } }

            InboundOverflowPolicy.DROP_LATEST ->
                log.warn("Inbound queue full (capacity={0}); dropping a message sessionId={1}",
                    options.inboundBufferCapacity, session.id)

            InboundOverflowPolicy.CLOSE -> {
                log.warn("Inbound queue full (capacity={0}); closing sessionId={1}",
                    options.inboundBufferCapacity, session.id)
                runCatching { session.close(CloseStatus.SESSION_NOT_RELIABLE) }
            }
        }
    }

    /**
     * One connection's lifecycle, start to finish, on a single coroutine.
     *
     * Mirrors `Route.kudosWebSocket` step for step so the two engines cannot drift on the details
     * that matter: reject before registering, always unregister, and run teardown under
     * [NonCancellable].
     */
    private suspend fun pump(raw: WebSocketSession, pump: Pump) {
        val session = try {
            sessionFactory.create(raw)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            log.warn("WebSocket sessionFactory threw, rejecting connection sessionId={0} cause={1}", raw.id, t.message)
            null
        }
        if (session == null) {
            log.debug("WebSocket connection rejected by sessionFactory sessionId={0}", raw.id)
            closeQuietly(raw, options.rejectCloseReason)
            return
        }

        val rejection = connectInterceptors.admit(session, raw.uri?.path.orEmpty())
        if (rejection != null) {
            runCatching { session.close(rejection.reason) }
            return
        }

        registry.register(session)
        var cause: Throwable? = null
        try {
            handler.onConnect(session)
            for (payload in pump.inbound) {
                when (payload) {
                    is WebSocketPayload.Text -> handler.onText(session, payload.text)
                    is WebSocketPayload.Binary -> handler.onBinary(session, payload.bytes)
                }
            }
        } catch (t: Throwable) {
            cause = t
            if (t is CancellationException) {
                log.debug("WebSocket session cancelled sessionId={0}", session.sessionId)
            } else {
                log.warn("WebSocket handling exception sessionId={0} cause={1}", session.sessionId, t.message)
            }
        } finally {
            withContext(NonCancellable) {
                runCatching { handler.onDisconnect(session, cause ?: pump.disconnectCause()) }
                    .onFailure { log.warn("onDisconnect threw an exception sessionId={0} cause={1}", session.sessionId, it.message) }
                registry.unregister(session)
                runCatching { session.close(WebSocketCloseReason.NORMAL) }
            }
        }
        if (cause is CancellationException) throw cause
    }

    private fun closeQuietly(raw: WebSocketSession, reason: WebSocketCloseReason) {
        runCatching { raw.close(CloseStatus(reason.code.toInt(), reason.message)) }
            .onFailure { log.debug("Closing rejected WebSocket sessionId={0} failed cause={1}", raw.id, it.message) }
    }

    /** Per-connection queue plus the bits of state the container callbacks hand back out of band. */
    private class Pump(val inbound: Channel<WebSocketPayload>) {

        @Volatile
        var transportError: Throwable? = null

        @Volatile
        var closeStatus: CloseStatus? = null

        /**
         * What to report as `onDisconnect`'s cause: the transport error if one was raised, otherwise a
         * synthetic exception for a non-normal close status.
         *
         * The synthetic one matters. A client that vanishes — laptop lid closed, cellular handover —
         * produces close code 1006 and **no** transport error, so reporting only [transportError]
         * would tell the business side that a dropped connection closed cleanly. Presence flags and
         * "was this a logout or a network blip" decisions are made on exactly this signal.
         */
        fun disconnectCause(): Throwable? {
            transportError?.let { return it }
            val status = closeStatus ?: return null
            return if (status.code == CloseStatus.NORMAL.code) null else WebSocketAbnormalCloseException(status)
        }
    }

    /**
     * Queue / send / shutdown limits for [KudosSpringWebSocketHandler].
     *
     * @property inboundBufferCapacity messages that may queue per connection before
     *   [inboundOverflow] applies. Sized for a burst, not a backlog: a deep queue only delays the
     *   moment backpressure reaches the client, while multiplying worst-case heap by the connection count.
     * @property inboundOverflow what a full queue does
     * @property sendTimeLimitMillis how long [ConcurrentWebSocketSessionDecorator] may spend on one send
     * @property sendBufferSizeLimitBytes how many bytes may queue for a client that is not reading
     * @property sendOverflowStrategy `TERMINATE` kills a session past the buffer limit; `DROP` discards
     *   its pending messages instead. Terminating is the default because a connection that cannot keep
     *   up has already lost messages — silently dropping them only hides that from both sides.
     * @property rejectCloseReason close reason used when [ISpringWebSocketSessionFactory] returns `null`
     * @property shutdownTimeoutMillis how long [destroy] waits for pumps to drain before cancelling
     */
    data class Options(
        val inboundBufferCapacity: Int = 64,
        val inboundOverflow: InboundOverflowPolicy = InboundOverflowPolicy.BACKPRESSURE,
        val sendTimeLimitMillis: Int = 10_000,
        val sendBufferSizeLimitBytes: Int = 512 * 1024,
        val sendOverflowStrategy: ConcurrentWebSocketSessionDecorator.OverflowStrategy =
            ConcurrentWebSocketSessionDecorator.OverflowStrategy.TERMINATE,
        val rejectCloseReason: WebSocketCloseReason = WebSocketCloseReason.REJECTED,
        val shutdownTimeoutMillis: Long = 10_000,
    )
}

/**
 * What [KudosSpringWebSocketHandler] does when a connection's inbound queue is full.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
enum class InboundOverflowPolicy {

    /**
     * Block the container thread until the queue drains, throttling the client through TCP flow
     * control. Loses nothing; costs one thread per backed-up connection. The right default, and
     * close to free on virtual threads.
     */
    BACKPRESSURE,

    /** Discard the message and log. Never blocks; loses data silently from the client's point of view. */
    DROP_LATEST,

    /** Close the connection. For endpoints where a client outrunning the server is abuse rather than load. */
    CLOSE,
}

/**
 * Reported as `onDisconnect`'s cause when a connection ended with a close status other than 1000 and
 * no transport error was raised — most often code 1006, the client that simply disappeared.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class WebSocketAbnormalCloseException(val closeStatus: CloseStatus) :
    RuntimeException("WebSocket closed abnormally: code=${closeStatus.code} reason=${closeStatus.reason.orEmpty()}")

/** Copies the buffer's remaining bytes without disturbing its position — the message may be read again by Spring. */
private fun java.nio.ByteBuffer.toByteArray(): ByteArray {
    val copy = duplicate()
    val bytes = ByteArray(copy.remaining())
    copy.get(bytes)
    return bytes
}
