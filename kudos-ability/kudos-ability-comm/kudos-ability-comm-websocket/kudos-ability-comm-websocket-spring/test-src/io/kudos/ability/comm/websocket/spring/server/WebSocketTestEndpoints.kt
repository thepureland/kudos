package io.kudos.ability.comm.websocket.spring.server

import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.spring.handler.ISpringWebSocketSessionFactory
import io.kudos.ability.comm.websocket.spring.handshake.HandshakeAttrs
import io.kudos.ability.comm.websocket.spring.handshake.IWebSocketHandshakeGuard
import io.kudos.ability.comm.websocket.spring.handshake.WebSocketHandshakeDecision
import io.kudos.ability.comm.websocket.spring.init.KudosWebSocketEndpoint
import io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * Endpoints mounted for [SpringWebSocketEndToEndTest]: one plain echo endpoint, one behind a
 * handshake guard, and one whose session factory refuses to identify the caller.
 *
 * Declared as real beans rather than assembled in the test, because the point of the end-to-end suite
 * is to prove the **wiring** — that `IKudosWebSocketEndpoint` beans reach the registry, the handshake
 * interceptor is mounted, and the container actually upgrades. Building the handlers by hand would
 * bypass exactly the code under test.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
open class WebSocketTestEndpoints {

    @Bean
    open fun echoHandler() = RecordingTestHandler()

    @Bean
    open fun notifyHandler() = RecordingTestHandler()

    /** Identifies the caller from `?token=`, the way a browser client has to (it cannot set headers). */
    @Bean
    open fun tokenSessionFactory() = ISpringWebSocketSessionFactory { raw ->
        val token = HandshakeAttrs.queryParam(raw, "token")
        if (token.isNullOrBlank()) null else SpringWebSocketSession(raw, userId = token, tenantId = "t1")
    }

    @Bean
    open fun echoEndpoint(echoHandler: RecordingTestHandler, factory: ISpringWebSocketSessionFactory) =
        KudosWebSocketEndpoint(ECHO_PATH, echoHandler, sessionFactory = factory)

    /** Second endpoint: proves multi-endpoint registration works over a real container. */
    @Bean
    open fun notifyEndpoint(notifyHandler: RecordingTestHandler, factory: ISpringWebSocketSessionFactory) =
        KudosWebSocketEndpoint(NOTIFY_PATH, notifyHandler, sessionFactory = factory)

    /** Refuses the upgrade itself, so the client gets an HTTP status instead of a socket that opens and closes. */
    @Bean
    open fun guardedEndpoint(echoHandler: RecordingTestHandler, factory: ISpringWebSocketSessionFactory) =
        KudosWebSocketEndpoint(
            GUARDED_PATH, echoHandler, sessionFactory = factory,
            handshakeGuards = listOf(IWebSocketHandshakeGuard { request, _ ->
                if (HandshakeAttrs.parseQuery(request.uri)["token"].isNullOrEmpty()) {
                    WebSocketHandshakeDecision.Reject(HttpStatus.UNAUTHORIZED, "missing token")
                } else {
                    WebSocketHandshakeDecision.Proceed
                }
            }),
        )

    /** Upgrade succeeds, identification does not — the connection must be closed and never registered. */
    @Bean
    open fun unidentifiableEndpoint(echoHandler: RecordingTestHandler) =
        KudosWebSocketEndpoint(REJECT_PATH, echoHandler, sessionFactory = { null })

    /**
     * SockJS fallback, opted in per-endpoint.
     *
     * Left on the **default** session factory on purpose, so this endpoint doubles as the end-to-end
     * check that an endpoint declaring no identification really does accept anonymous connections — the
     * documented default, otherwise only covered by a unit test.
     */
    @Bean
    open fun sockJsEndpoint(sockJsHandler: RecordingTestHandler) =
        KudosWebSocketEndpoint(SOCKJS_PATH, sockJsHandler, sockJs = true)

    @Bean
    open fun sockJsHandler() = RecordingTestHandler()

    companion object {
        const val ECHO_PATH = "/ws/echo"
        const val NOTIFY_PATH = "/ws/notify"
        const val GUARDED_PATH = "/ws/guarded"
        const val REJECT_PATH = "/ws/reject"
        const val SOCKJS_PATH = "/ws/sockjs"
    }
}

/**
 * Server-side handler that records what it saw and echoes text back, so a client can assert a full
 * round trip.
 *
 * Queues rather than plain lists: the hooks run on the pump coroutine while assertions run on the test
 * thread, and a `poll(timeout)` expresses "wait for the server to get there" without a sleep.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RecordingTestHandler : IKudosWebSocketHandler {

    val connectedUserIds = LinkedBlockingQueue<String>()
    val texts = LinkedBlockingQueue<String>()
    val disconnectedSessionIds = LinkedBlockingQueue<String>()

    /** sessionId → whether the disconnect carried a cause; `onDisconnect(cause = null)` means a clean close. */
    val disconnectHadCause = ConcurrentHashMap<String, Boolean>()

    override suspend fun onConnect(session: KudosWebSocketSessionRef) {
        connectedUserIds += (session.userId ?: ANONYMOUS)
    }

    override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
        texts += text
        session.sendText("echo:$text")
    }

    override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {
        session.sendBinary(bytes)
    }

    override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
        disconnectHadCause[session.sessionId] = cause != null
        disconnectedSessionIds += session.sessionId
    }

    companion object {
        const val ANONYMOUS = "<anonymous>"
    }
}
