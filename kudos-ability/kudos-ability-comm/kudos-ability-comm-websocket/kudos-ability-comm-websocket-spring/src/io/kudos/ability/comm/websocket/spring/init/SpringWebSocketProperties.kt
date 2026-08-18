package io.kudos.ability.comm.websocket.spring.init

import io.kudos.ability.comm.websocket.spring.handler.InboundOverflowPolicy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator

/**
 * Configuration for the Spring engine module, bound from `kudos.ability.comm.websocket.spring`.
 *
 * Sits alongside — not instead of — `kudos.ability.comm.websocket.*`, which
 * [io.kudos.ability.comm.websocket.common.init.WebSocketProperties] binds and which covers the
 * engine-neutral concerns (broadcast limits, cross-process delivery). Everything here is specific to
 * running on a servlet container.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.comm.websocket.spring")
open class SpringWebSocketProperties {

    /**
     * Whether to register WebSocket endpoints at all.
     *
     * Set `false` in an application that depends on this module for its types but mounts endpoints
     * itself — or to switch the whole feature off without removing the dependency.
     */
    var enabled: Boolean = true

    /**
     * Path used for the fallback single endpoint — the one registered when the application declares
     * exactly one [io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler] bean and no
     * [IKudosWebSocketEndpoint] beans. Ignored once endpoints are declared explicitly.
     */
    var path: String = "/ws"

    /**
     * Exact origins allowed to open a connection. **Empty means Spring's default: same-origin only.**
     *
     * That default is deliberate and worth stating, because the obvious-looking `"*"` is a real
     * vulnerability rather than a convenience. A WebSocket handshake is not subject to the same-origin
     * policy the way `fetch` is, and the browser sends the user's cookies with it; allowing every
     * origin therefore lets any page on the internet open an authenticated socket as your logged-in
     * user, read whatever it pushes and send whatever it accepts — the WebSocket form of CSRF, with no
     * preflight to stop it. [SpringWebSocketAutoConfiguration] logs a startup warning if `"*"` is
     * configured anyway.
     */
    var allowedOrigins: List<String> = emptyList()

    /** Origin patterns — a scheme plus a wildcard host such as `*.example.com`; consulted only when [allowedOrigins] is empty. */
    var allowedOriginPatterns: List<String> = emptyList()

    /**
     * Whether to expose a SockJS fallback alongside the native endpoint.
     *
     * Off by default. Every browser and HTTP client in support has had native WebSocket for a decade,
     * and SockJS costs an extra URL surface plus its own session/heartbeat machinery. Turn it on for
     * clients stuck behind a proxy that strips the `Upgrade` header.
     */
    var sockJs: Boolean = false

    /** How long [SpringWebSocketAutoConfiguration] waits for in-flight messages at shutdown. */
    var shutdownTimeoutMillis: Long = 10_000

    /** Per-connection inbound queue. */
    var inbound: Inbound = Inbound()

    /** Outbound send limits, applied through [ConcurrentWebSocketSessionDecorator]. */
    var send: Send = Send()

    /** Servlet container WebSocket limits, applied through a `ServletServerContainerFactoryBean`. */
    var container: Container = Container()

    /**
     * Per-connection inbound queue, drained by that connection's single pump coroutine.
     *
     * See [io.kudos.ability.comm.websocket.spring.handler.KudosSpringWebSocketHandler] for why the
     * queue exists at all (ordering + releasing the container thread) and what the policies cost.
     *
     * @property bufferCapacity messages that may queue before [overflow] applies. Sized for a burst,
     *   not a backlog — a deeper queue only postpones backpressure while multiplying worst-case heap
     *   by the connection count.
     * @property overflow what a full queue does
     */
    open class Inbound {
        var bufferCapacity: Int = 64
        var overflow: InboundOverflowPolicy = InboundOverflowPolicy.BACKPRESSURE
    }

    /**
     * Outbound limits.
     *
     * @property timeLimitMillis how long one send may take before the session is considered stuck
     * @property bufferSizeLimitBytes how many bytes may queue for a client that has stopped reading.
     *   This is the setting that stands between one unresponsive client and an out-of-memory error:
     *   without a limit, a session that never drains its socket accumulates every message ever sent to it.
     * @property overflowStrategy `TERMINATE` closes a session past the limit, `DROP` discards its
     *   pending messages. Terminating is the default — a connection that cannot keep up has already
     *   lost messages, and dropping silently hides that from both ends.
     */
    open class Send {
        var timeLimitMillis: Int = 10_000
        var bufferSizeLimitBytes: Int = 512 * 1024
        var overflowStrategy: ConcurrentWebSocketSessionDecorator.OverflowStrategy =
            ConcurrentWebSocketSessionDecorator.OverflowStrategy.TERMINATE
    }

    /**
     * Servlet container limits.
     *
     * These matter more here than on Ktor. Because `supportsPartialMessages` is `false`, the container
     * reassembles fragments into one message before kudos sees it — so the container's buffer size,
     * not any check in this module, is what bounds how much memory one client can make the server
     * allocate for a single message. The Jakarta WebSocket default is 8 KB, which silently truncates
     * larger business payloads.
     *
     * @property maxTextMessageBufferSize largest reassembled text message
     * @property maxBinaryMessageBufferSize largest reassembled binary message
     * @property maxSessionIdleTimeoutMillis idle timeout; `0` leaves the container default alone.
     *   Set it above the client's heartbeat interval or healthy connections get culled.
     * @property asyncSendTimeoutMillis container-level async send timeout; `0` leaves the default alone
     */
    open class Container {
        var maxTextMessageBufferSize: Int = 64 * 1024
        var maxBinaryMessageBufferSize: Int = 64 * 1024
        var maxSessionIdleTimeoutMillis: Long = 0
        var asyncSendTimeoutMillis: Long = 0
    }
}
