package io.kudos.ability.comm.websocket.ktor.distributed

import io.kudos.ability.comm.websocket.ktor.broadcast.IWebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope.TargetType
import io.kudos.ability.comm.websocket.ktor.session.WebSocketPayload
import io.kudos.base.logger.LogFactory

/**
 * Cross-process WebSocket broadcaster.
 *
 * Decorates a process-local [IWebSocketBroadcaster] so that every broadcast call:
 *  1. Publishes a [WebSocketBroadcastEnvelope] onto an [IWebSocketBroadcastChannel] for other nodes to pick up.
 *  2. Delivers the message to local sessions on this node via the wrapped [local] broadcaster.
 *
 * Because both sides implement [IWebSocketBroadcaster], business code that declares the interface
 * moves from single-instance to clustered with no call-site changes.
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
 * **[unicast] is local-first**: a sessionId identifies exactly one connection cluster-wide, so a local
 * hit means no other node can hold it and publishing would be pure waste — every node in the fleet
 * would deserialize an envelope addressed to a session it does not have. Only a local miss falls
 * through to the channel.
 *
 * **Publish failures are logged + swallowed**: a channel publish exception logs WARN and continues to
 * the local broadcast. The design choice favors keeping local users functional when Redis is briefly
 * unavailable; the alternative (fail the whole send) would couple local liveness to remote infra
 * uptime, which is rarely what a WebSocket app wants.
 *
 * Closing this broadcaster detaches its channel subscription; it does not close the channel itself,
 * which is typically shared and owned by the wiring layer.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DistributedWebSocketBroadcaster(
    private val local: IWebSocketBroadcaster,
    private val channel: IWebSocketBroadcastChannel,
    /** Stable identifier for this process; used to filter pub/sub self-deliveries. Typically a UUID. */
    private val nodeId: String,
) : IWebSocketBroadcaster, AutoCloseable {

    private val log = LogFactory.getLog(this::class)

    /**
     * Declared last on purpose: subscribing publishes `this` to another object, so every property it
     * touches must already be initialized when the first envelope arrives.
     */
    private val subscription: WebSocketBroadcastSubscription = channel.subscribe(::onInbound)

    /** Broadcasts to every locally-registered session and every session on remote nodes. */
    override suspend fun broadcast(payload: WebSocketPayload): Int {
        publish(TargetType.ALL, targetId = null, payload = payload)
        return local.broadcast(payload)
    }

    /** Broadcasts to every session whose `userId` matches [userId], locally and on every remote node. */
    override suspend fun broadcastToUser(userId: String, payload: WebSocketPayload): Int {
        publish(TargetType.USER, targetId = userId, payload = payload)
        return local.broadcastToUser(userId, payload)
    }

    /** Broadcasts to every session whose `tenantId` matches [tenantId], locally and on every remote node. */
    override suspend fun broadcastToTenant(tenantId: String, payload: WebSocketPayload): Int {
        publish(TargetType.TENANT, targetId = tenantId, payload = payload)
        return local.broadcastToTenant(tenantId, payload)
    }

    /**
     * Unicasts to a single sessionId, trying this node first and only publishing on a local miss.
     * Returns whether the **local** registry held the session; a `false` return does not mean the
     * message was undelivered — another node may well hold that session. Callers needing
     * global-success semantics should tag sessions with a userId and use [broadcastToUser].
     */
    override suspend fun unicast(sessionId: String, payload: WebSocketPayload): Boolean {
        if (local.unicast(sessionId, payload)) return true
        publish(TargetType.SESSION, targetId = sessionId, payload = payload)
        return false
    }

    /** Detaches the channel subscription. The channel itself is owned by the wiring layer. */
    override fun close() {
        runCatching { subscription.close() }
            .onFailure { log.warn("Detaching distributed WebSocket subscription failed nodeId={0} cause={1}", nodeId, it.message) }
    }

    private suspend fun publish(targetType: TargetType, targetId: String?, payload: WebSocketPayload) {
        val envelope = WebSocketBroadcastEnvelope.of(nodeId, targetType, targetId, payload)
        runCatching { channel.publish(envelope) }
            .onFailure { log.warn("Distributed WebSocket publish failed targetType={0} targetId={1} cause={2}", targetType, targetId, it.message) }
    }

    /**
     * Channel handler. Filters self-echoes by [nodeId] and dispatches to the local broadcaster — never
     * re-publishes, otherwise nodes would echo each other into a fan-out storm.
     */
    private suspend fun onInbound(envelope: WebSocketBroadcastEnvelope) {
        if (envelope.nodeId == nodeId) return
        if (envelope.version > WebSocketBroadcastEnvelope.CURRENT_VERSION) {
            // A newer node in a partially-upgraded fleet. Dropping is safer than guessing at fields
            // this build cannot interpret, and the WARN tells the operator the rollout is mixed.
            log.warn("Dropping WebSocket broadcast envelope of unsupported version={0} (supported<={1}) from nodeId={2}",
                envelope.version, WebSocketBroadcastEnvelope.CURRENT_VERSION, envelope.nodeId)
            return
        }
        val payload = envelope.payload
        if (payload == null) {
            log.warn("Dropping WebSocket broadcast envelope without payload from nodeId={0} targetType={1}",
                envelope.nodeId, envelope.targetType)
            return
        }
        try {
            when (envelope.targetType) {
                TargetType.ALL -> local.broadcast(payload)
                TargetType.USER -> envelope.targetId?.let { local.broadcastToUser(it, payload) }
                TargetType.TENANT -> envelope.targetId?.let { local.broadcastToTenant(it, payload) }
                TargetType.SESSION -> envelope.targetId?.let { local.unicast(it, payload) }
            }
        } catch (t: Throwable) {
            // An unhandled throw inside the channel listener would, on Redis, terminate the
            // listener container's worker thread and stop all subsequent deliveries on this node.
            // Catch + ERROR keeps the channel alive while making the operator aware.
            log.error(t, "Distributed WebSocket inbound delivery failed targetType={0} targetId={1}", envelope.targetType, envelope.targetId)
        }
    }
}
