package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.ktor.websocket.CloseReason

/**
 * Admission control for a newly established connection.
 *
 * Runs **after** the route's `sessionFactory` built the session and **before** the session is put
 * into the registry, which is the only window where identity is known but the connection is not yet
 * reachable by broadcasts. That ordering is deliberate:
 *
 *  - Before the factory, `userId` / `tenantId` do not exist yet, so per-user device caps and
 *    per-tenant connection quotas — the things admission control is actually for — could not be
 *    expressed.
 *  - After registration it is too late: for the window until the connection is closed the session
 *    is already a member of its claimed tenant's broadcast group and will receive that tenant's
 *    messages.
 *
 * `sessionFactory` returning `null` remains the seam for "cannot even identify this connection"
 * (bad or missing credentials); this interface is for "identified, but not admitted". Splitting them
 * keeps the factory a pure constructor instead of a place where construction and policy blur
 * together, and — unlike a single factory lambda — a list of interceptors composes: independent
 * concerns stay independent, ordering is explicit, and the first rejection wins.
 *
 * Protocol-level concerns that must be settled *before* the handshake (token validation, Origin
 * checks) belong in Ktor's own plugins — `authenticate("jwt") { kudosWebSocket(...) }` works because
 * `kudosWebSocket` is an ordinary `Route` extension. This module deliberately does not reimplement
 * that layer.
 *
 * An interceptor that throws is treated as a rejection: admission control fails closed, because a
 * quota check that errors out must not be read as "quota available".
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface IWebSocketConnectInterceptor {

    /**
     * Decides whether [session] may join the registry.
     *
     * Typed as [KudosWebSocketSessionRef] for the same reason the registry, broadcaster and handler
     * SPI are: an implementation stays unit-testable against a plain stub, and survives a non-Ktor
     * engine. A check that needs the request itself (Origin, client IP, principal) casts to
     * [io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession] and reads `raw.call` —
     * though most of that belongs in a Ktor plugin ahead of the handshake instead.
     */
    suspend fun intercept(session: KudosWebSocketSessionRef): WebSocketConnectDecision
}

/**
 * Outcome of an [IWebSocketConnectInterceptor].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
sealed interface WebSocketConnectDecision {

    /** Admit the connection; remaining interceptors still run. */
    data object Proceed : WebSocketConnectDecision

    /**
     * Refuse the connection. No further interceptor runs, the session is never registered, and no
     * handler hook fires; the connection is closed with [reason].
     */
    data class Reject(val reason: CloseReason) : WebSocketConnectDecision {

        constructor(code: CloseReason.Codes, message: String) : this(CloseReason(code, message))
    }
}
