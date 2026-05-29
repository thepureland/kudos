package io.kudos.ability.comm.websocket.ktor.distributed

import io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope.TargetType
import io.kudos.base.logger.LogFactory

/**
 * Cross-process WebSocket broadcaster.
 *
 * Decorates a process-local [WebSocketBroadcaster] so that every broadcast call:
 *  1. Publishes a [WebSocketBroadcastEnvelope] onto an [IWebSocketBroadcastChannel] for other nodes to pick up.
 *  2. Delivers the message to local sessions on this node via the wrapped [local] broadcaster.
 *
 * On the receiving side, the channel subscription handler unwraps the envelope and calls back into the
 * **local** broadcaster only — it must not re-publish, or every node would echo each other's traffic.
 *
 * **Self-echo filter**: Redis pub/sub (and most transports) deliver published messages back to the
 * publisher. This class compares `envelope.nodeId` to its own [nodeId] and silently drops self-deliveries
 * to avoid a duplicate fan-out on the originating node (which already ran the local broadcast as step 2).
 *
 * **Return values are local-only**: `broadcast*` returns the number of *local* sessions that received
 * the message, and `unicast` returns whether the *local* registry held the session. The remote
 * deliveries are fire-and-forget — measuring global success would require an ack protocol the channel
 * SPI does not provide and most transports cannot guarantee.
 *
 * **Publish failures are logged + swallowed**: a channel publish exception logs WARN and continues to
 * the local broadcast. The design choice favors keeping local users functional when Redis is briefly
 * unavailable; the alternative (fail the whole send) would couple local liveness to remote infra
 * uptime, which is rarely what a WebSocket app wants.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DistributedWebSocketBroadcaster(
    private val local: WebSocketBroadcaster,
    private val channel: IWebSocketBroadcastChannel,
    /** Stable identifier for this process; used to filter pub/sub self-deliveries. Typically a UUID. */
    private val nodeId: String,
) {

    private val log = LogFactory.getLog(this::class)

    init {
        channel.subscribe { envelope -> onInbound(envelope) }
    }

    /** Broadcasts to every locally-registered session and every session on remote nodes. */
    suspend fun broadcast(text: String): Int {
        publish(TargetType.ALL, targetId = null, text = text)
        return local.broadcast(text)
    }

    /** Broadcasts to every session whose `userId` matches [userId], locally and on every remote node. */
    suspend fun broadcastToUser(userId: String, text: String): Int {
        publish(TargetType.USER, targetId = userId, text = text)
        return local.broadcastToUser(userId, text)
    }

    /** Broadcasts to every session whose `tenantId` matches [tenantId], locally and on every remote node. */
    suspend fun broadcastToTenant(tenantId: String, text: String): Int {
        publish(TargetType.TENANT, targetId = tenantId, text = text)
        return local.broadcastToTenant(tenantId, text)
    }

    /**
     * Unicasts to a single sessionId. Returns whether the **local** registry held the session — even
     * when returning `false`, the envelope is still published so another node holding that session
     * can deliver it. Callers needing global-success semantics should switch to broadcastToUser by
     * tagging sessions with a userId.
     */
    suspend fun unicast(sessionId: String, text: String): Boolean {
        publish(TargetType.SESSION, targetId = sessionId, text = text)
        return local.unicast(sessionId, text)
    }

    private suspend fun publish(targetType: TargetType, targetId: String?, text: String) {
        val envelope = WebSocketBroadcastEnvelope(
            nodeId = nodeId,
            targetType = targetType,
            targetId = targetId,
            text = text,
        )
        runCatching { channel.publish(envelope) }
            .onFailure { log.warn("Distributed WebSocket publish failed targetType={0} targetId={1} cause={2}", targetType, targetId, it.message) }
    }

    /**
     * Channel handler. Filters self-echoes by [nodeId] and dispatches to the local broadcaster — never
     * re-publishes, otherwise nodes would echo each other into a fan-out storm.
     */
    private suspend fun onInbound(envelope: WebSocketBroadcastEnvelope) {
        if (envelope.nodeId == nodeId) return
        try {
            when (envelope.targetType) {
                TargetType.ALL -> local.broadcast(envelope.text)
                TargetType.USER -> envelope.targetId?.let { local.broadcastToUser(it, envelope.text) }
                TargetType.TENANT -> envelope.targetId?.let { local.broadcastToTenant(it, envelope.text) }
                TargetType.SESSION -> envelope.targetId?.let { local.unicast(it, envelope.text) }
            }
        } catch (t: Throwable) {
            // An unhandled throw inside the channel listener would, on Redis, terminate the
            // listener container's worker thread and stop all subsequent deliveries on this node.
            // Catch + ERROR keeps the channel alive while making the operator aware.
            log.error(t, "Distributed WebSocket inbound delivery failed targetType={0} targetId={1}", envelope.targetType, envelope.targetId)
        }
    }
}
