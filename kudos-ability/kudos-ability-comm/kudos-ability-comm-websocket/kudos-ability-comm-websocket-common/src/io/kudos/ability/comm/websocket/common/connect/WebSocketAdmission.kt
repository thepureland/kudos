package io.kudos.ability.comm.websocket.common.connect

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import io.kudos.base.logger.LogFactory
import kotlinx.coroutines.CancellationException

/** Private logger-category anchor: this file holds only top-level functions, so a dedicated object keeps log events attributed to admission control rather than an unrelated class. */
private object WebSocketAdmission

private val log = LogFactory.getLog(WebSocketAdmission::class)

/**
 * Runs these interceptors in order and returns the first rejection, or `null` when [session] is
 * admitted.
 *
 * Lives in the engine-neutral module because both engine adapters need exactly this, at exactly the
 * same point in the lifecycle — after identity is established, before the session enters the
 * registry. Keeping one copy is not just less code: the fail-closed rule below is a security
 * property, and two hand-maintained copies of a security property drift.
 *
 * An interceptor that throws is converted into a rejection rather than propagated: **admission
 * control fails closed**, because a quota or ban check that blew up must not be read as "allowed".
 * Cancellation is not an interceptor failure and is re-thrown, so a shutdown still unwinds.
 *
 * @param endpoint route / path, used only to make the log line identify which endpoint refused
 * @return the winning rejection, or `null` to admit
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
suspend fun List<IWebSocketConnectInterceptor>.admit(
    session: KudosWebSocketSessionRef,
    endpoint: String = "",
): WebSocketConnectDecision.Reject? {
    for (interceptor in this) {
        val decision = try {
            interceptor.intercept(session)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            log.warn("WebSocket connect interceptor {0} threw, rejecting connection sessionId={1} endpoint={2} cause={3}",
                interceptor::class.simpleName, session.sessionId, endpoint, t.message)
            WebSocketConnectDecision.Reject(WebSocketCloseReason.Codes.INTERNAL_ERROR, "Admission check failed")
        }
        if (decision is WebSocketConnectDecision.Reject) {
            log.debug("WebSocket connection rejected by {0} sessionId={1} endpoint={2}",
                interceptor::class.simpleName, session.sessionId, endpoint)
            return decision
        }
    }
    return null
}
