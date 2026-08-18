package io.kudos.ability.comm.websocket.common.init

/**
 * WebSocket business-layer configuration properties, corresponding to
 * `kudos.ability.comm.websocket.*`.
 *
 * @property nodeId this process's identity in a cluster; blank means "generate a random one at startup"
 * @property broadcast fan-out safety limits
 * @property distributed cross-process broadcasting
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class WebSocketProperties(
    var nodeId: String = "",
    var broadcast: Broadcast = Broadcast(),
    var distributed: Distributed = Distributed(),
) {

    /**
     * Limits that keep one slow client from taking the process down.
     * See [io.kudos.ability.comm.websocket.common.broadcast.WebSocketBroadcaster] for the rationale.
     *
     * @property sendTimeoutMillis per-session send deadline
     * @property maxConcurrentSends in-flight send cap for a single broadcaster
     * @property closeOnSendTimeout close a connection whose send timed out (it is not draining its socket)
     */
    data class Broadcast(
        var sendTimeoutMillis: Long = 10_000,
        var maxConcurrentSends: Int = 512,
        var closeOnSendTimeout: Boolean = true,
    )

    /**
     * Cross-process broadcasting. Off by default: a single-instance deployment should not need Redis.
     *
     * @property enabled turn on the Redis-backed [io.kudos.ability.comm.websocket.common.distributed.DistributedWebSocketBroadcaster]
     * @property redisChannel pub/sub channel name; every node in a deployment must agree on it
     * @property inboundBufferCapacity how many inbound envelopes may queue before delivery starts dropping
     */
    data class Distributed(
        var enabled: Boolean = false,
        var redisChannel: String = "kudos:ws:broadcast",
        var inboundBufferCapacity: Int = 1024,
    )
}
