package io.kudos.ability.comm.websocket.common.connect

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.base.logger.LogFactory
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason

/**
 * Caps how many simultaneous connections one user / one tenant / this node may hold.
 *
 * Counts come from [KudosWebSocketRegistry]'s existing indexes, so the check costs a map lookup and
 * needs no state of its own.
 *
 * **Best-effort, not a hard limit.** The count is read before the session is registered, so N
 * connections racing through this check at once can all observe "under the cap" and all be admitted,
 * overshooting by up to N-1. Making it exact would mean holding a lock across admission and
 * registration, on the connection path, to defend against an overshoot that is bounded by
 * concurrency and self-corrects as connections close. A hard ceiling belongs at the gateway or load
 * balancer; this guard exists to stop a runaway client from opening thousands of sockets.
 *
 * A limit of `0` or less disables that dimension. Anonymous sessions (null `userId` / `tenantId`)
 * skip the corresponding dimension — there is no meaningful identity to count them against.
 *
 * @param registry source of the current connection counts
 * @param maxPerUser simultaneous connections allowed per `userId` (multi-device); 0 disables
 * @param maxPerTenant simultaneous connections allowed per `tenantId`; 0 disables
 * @param maxTotal simultaneous connections allowed on this node; 0 disables
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class MaxConnectionsInterceptor(
    private val registry: KudosWebSocketRegistry,
    private val maxPerUser: Int = DEFAULT_MAX_PER_USER,
    private val maxPerTenant: Int = 0,
    private val maxTotal: Int = 0,
) : IWebSocketConnectInterceptor {

    private val log = LogFactory.getLog(this::class)

    override suspend fun intercept(session: KudosWebSocketSessionRef): WebSocketConnectDecision {
        if (maxTotal > 0 && registry.size >= maxTotal) {
            return reject(session, "node", maxTotal, registry.size)
        }
        val userId = session.userId
        if (maxPerUser > 0 && userId != null) {
            val current = registry.findByUserId(userId).size
            if (current >= maxPerUser) return reject(session, "user $userId", maxPerUser, current)
        }
        val tenantId = session.tenantId
        if (maxPerTenant > 0 && tenantId != null) {
            val current = registry.findByTenantId(tenantId).size
            if (current >= maxPerTenant) return reject(session, "tenant $tenantId", maxPerTenant, current)
        }
        return WebSocketConnectDecision.Proceed
    }

    private fun reject(
        session: KudosWebSocketSessionRef,
        scope: String,
        limit: Int,
        current: Int,
    ): WebSocketConnectDecision {
        log.warn("Rejecting WebSocket connection sessionId={0}: {1} already holds {2} connections (limit {3})",
            session.sessionId, scope, current, limit)
        // TRY_AGAIN_LATER rather than VIOLATED_POLICY: the client did nothing wrong and a retry
        // after other connections drain is the correct client behavior.
        return WebSocketConnectDecision.Reject(WebSocketCloseReason.Codes.TRY_AGAIN_LATER, "Too many connections")
    }

    companion object {
        /** Generous enough for a browser + a phone + a spare tab, tight enough to stop a reconnect storm. */
        const val DEFAULT_MAX_PER_USER: Int = 8
    }
}
