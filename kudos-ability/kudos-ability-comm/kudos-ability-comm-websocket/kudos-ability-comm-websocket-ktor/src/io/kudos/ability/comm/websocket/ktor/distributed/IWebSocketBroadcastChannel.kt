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
 * - Implementations should deliver envelopes to a given handler **in publish order** where the
 *   transport preserves order, and must not let a slow handler block the transport's receive path.
 *
 * Lifecycle: [subscribe] returns a handle so a subscriber can detach, and implementations holding
 * transport resources should implement [AutoCloseable] (Spring calls `close()` on such beans at
 * shutdown). Without a way to unsubscribe, every test, hot reload or context refresh leaks its
 * handlers into a channel that lives as long as the process.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IWebSocketBroadcastChannel {

    /** Publishes an envelope to every node, including the originating one (subscribers must filter). */
    suspend fun publish(envelope: WebSocketBroadcastEnvelope)

    /**
     * Registers a handler that receives every envelope delivered by the transport.
     *
     * @return a handle that detaches this handler when closed
     */
    fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit): WebSocketBroadcastSubscription
}

/**
 * Handle for one [IWebSocketBroadcastChannel.subscribe] registration. Closing it detaches the
 * handler; closing twice is a no-op.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface WebSocketBroadcastSubscription : AutoCloseable {
    override fun close()
}
