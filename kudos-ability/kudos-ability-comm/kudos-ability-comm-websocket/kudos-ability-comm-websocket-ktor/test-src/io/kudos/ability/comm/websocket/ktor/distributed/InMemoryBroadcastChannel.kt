package io.kudos.ability.comm.websocket.ktor.distributed

import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory [IWebSocketBroadcastChannel] used for unit tests.
 *
 * Models the transport semantics that matter for [DistributedWebSocketBroadcaster]: every
 * `publish(...)` is delivered synchronously to every registered subscriber, including the publisher
 * itself (so the self-echo filter inside the decorator is exercised end-to-end, not bypassed by the
 * test fixture).
 *
 * Test setups usually pair this channel with two [DistributedWebSocketBroadcaster] instances —
 * "node A" and "node B" — each backed by its own [io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry]
 * and [io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster]. Both subscribe to the
 * same channel instance; a publish from A then triggers B's local broadcast and vice versa.
 */
internal class InMemoryBroadcastChannel : IWebSocketBroadcastChannel {

    private val subscribers = CopyOnWriteArrayList<suspend (WebSocketBroadcastEnvelope) -> Unit>()

    override suspend fun publish(envelope: WebSocketBroadcastEnvelope) {
        for (handler in subscribers) {
            handler(envelope)
        }
    }

    override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit) {
        subscribers += handler
    }
}
