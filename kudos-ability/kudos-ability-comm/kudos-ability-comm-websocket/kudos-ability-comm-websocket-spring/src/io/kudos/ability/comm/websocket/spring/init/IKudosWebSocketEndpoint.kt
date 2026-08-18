package io.kudos.ability.comm.websocket.spring.init

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.spring.handler.ISpringWebSocketSessionFactory
import io.kudos.ability.comm.websocket.spring.handshake.IWebSocketHandshakeGuard

/**
 * One WebSocket endpoint, declared as a Spring bean.
 *
 * Publishing endpoints as beans is what lets an application host several of them — `/ws/chat`,
 * `/ws/notify`, `/ws/telemetry` — each with its own handler, its own identification and its own
 * quotas, instead of the single hardcoded path the reference implementation this module replaces
 * offered. [SpringWebSocketAutoConfiguration] picks every one of these up and registers it.
 *
 * ```kotlin
 * @Bean
 * fun chatEndpoint(chat: ChatHandler, factory: ISpringWebSocketSessionFactory) =
 *     KudosWebSocketEndpoint("/ws/chat", chat, sessionFactory = factory)
 * ```
 *
 * The nullable overrides ([allowedOrigins], [sockJs]) mean "inherit the module-wide setting"; a
 * non-null value wins for this endpoint only. That distinction is why they are nullable rather than
 * defaulted — an empty origin list is itself meaningful (Spring's same-origin default), so it cannot
 * double as "unset".
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IKudosWebSocketEndpoint {

    /** Path the endpoint is mounted at, e.g. `/ws/chat`. */
    val path: String

    /** Business SPI implementation driven by this endpoint. */
    val handler: IKudosWebSocketHandler

    /**
     * Identification seam. Defaults to an anonymous session — usable for a public feed, but note that
     * an anonymous session has no `userId` / `tenantId`, so it can only ever be reached by
     * `broadcast` and `unicast`, never by `broadcastToUser` / `broadcastToTenant`.
     */
    val sessionFactory: ISpringWebSocketSessionFactory
        get() = ISpringWebSocketSessionFactory { io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession(it) }

    /**
     * Post-identification admission checks for this endpoint. Empty means the module-wide
     * [IWebSocketConnectInterceptor] beans apply; a non-empty list replaces them.
     */
    val connectInterceptors: List<IWebSocketConnectInterceptor>
        get() = emptyList()

    /**
     * Pre-upgrade guards for this endpoint. Empty means the module-wide [IWebSocketHandshakeGuard]
     * beans apply; a non-empty list replaces them.
     */
    val handshakeGuards: List<IWebSocketHandshakeGuard>
        get() = emptyList()

    /** Exact allowed origins; `null` inherits `kudos.ability.comm.websocket.spring.allowed-origins`. */
    val allowedOrigins: List<String>?
        get() = null

    /** Whether to expose a SockJS fallback; `null` inherits `kudos.ability.comm.websocket.spring.sock-js`. */
    val sockJs: Boolean?
        get() = null
}

/**
 * Straightforward [IKudosWebSocketEndpoint] for the common case, so declaring an endpoint does not
 * require writing a class.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class KudosWebSocketEndpoint(
    override val path: String,
    override val handler: IKudosWebSocketHandler,
    override val sessionFactory: ISpringWebSocketSessionFactory =
        ISpringWebSocketSessionFactory { io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession(it) },
    override val connectInterceptors: List<IWebSocketConnectInterceptor> = emptyList(),
    override val handshakeGuards: List<IWebSocketHandshakeGuard> = emptyList(),
    override val allowedOrigins: List<String>? = null,
    override val sockJs: Boolean? = null,
) : IKudosWebSocketEndpoint
