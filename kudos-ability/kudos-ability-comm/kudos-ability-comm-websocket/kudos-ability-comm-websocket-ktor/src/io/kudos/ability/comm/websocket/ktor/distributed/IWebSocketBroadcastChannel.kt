package io.kudos.ability.comm.websocket.ktor.distributed

/**
 * Cross-process broadcast channel for WebSocket messages.
 *
 * Implementations transport a [WebSocketBroadcastEnvelope] from one node to every other node in the
 * deployment. Typical impls: Redis pub/sub (production), in-memory loopback (tests), Kafka topic /
 * NATS subject (alternative production transports).
 *
 * Contract:
 * - [publish] is fire-and-forget; it must not block the calling coroutine for an unbounded time.
 *   Failures should be reported via exception so the caller can decide whether to fall back to
 *   local-only delivery. Implementations may also log internally.
 * - [subscribe] registers a handler that receives every envelope delivered by the underlying
 *   transport, **including** envelopes published by this very node. Self-echo filtering is the
 *   subscriber's responsibility (compare `envelope.nodeId` to the local node id) so that a future
 *   transport without natural self-suppression (Kafka, NATS) does not silently break the contract.
 * - Multiple [subscribe] calls register additional handlers; implementations should fan out to all
 *   of them. In practice [DistributedWebSocketBroadcaster] subscribes once per instance.
 *
 * Lifecycle: implementations decide when the underlying transport starts / stops. The interface does
 * not expose `start` / `close` because the wiring layer (Spring auto-config, Ktor `Application`
 * lifecycle) is the natural owner.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IWebSocketBroadcastChannel {

    /** Publishes an envelope to every node, including the originating one (subscribers must filter). */
    suspend fun publish(envelope: WebSocketBroadcastEnvelope)

    /** Registers a handler that receives every envelope delivered by the transport. */
    fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit)
}
