package io.kudos.ability.comm.websocket.ktor.broadcast

import io.kudos.ability.comm.websocket.ktor.session.WebSocketPayload

/**
 * Fan-out contract shared by the process-local [WebSocketBroadcaster] and the cross-process
 * [io.kudos.ability.comm.websocket.ktor.distributed.DistributedWebSocketBroadcaster].
 *
 * Having both implement one interface is what makes the distributed decorator actually
 * substitutable: business code declares `IWebSocketBroadcaster`, and moving a service from one
 * instance to a cluster becomes a wiring change (see
 * [io.kudos.ability.comm.websocket.ktor.init.WebSocketAutoConfiguration]) instead of an edit to
 * every call site.
 *
 * Implementations take a [WebSocketPayload] so text and binary do not double the method count;
 * the `String` / `ByteArray` overloads below are conveniences that keep call sites readable.
 *
 * **Return values count local deliveries.** For the distributed implementation the remote fan-out
 * is fire-and-forget: measuring global success would need an ack protocol that most transports
 * cannot offer. Treat the result as "how many sessions on this node got it", not as a receipt.
 *
 * **A failed send never fails the call.** One dead or slow session is caught, logged and counted
 * as a miss so the rest of the fan-out completes.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IWebSocketBroadcaster {

    /** Sends to every connected session. Returns the number of sessions reached locally. */
    suspend fun broadcast(payload: WebSocketPayload): Int

    /** Sends to every session of [userId] (multi-device online scenario). Returns the number reached locally. */
    suspend fun broadcastToUser(userId: String, payload: WebSocketPayload): Int

    /** Sends to every session of [tenantId]. Returns the number reached locally. */
    suspend fun broadcastToTenant(tenantId: String, payload: WebSocketPayload): Int

    /** Sends to a single session. Returns whether **this node** delivered it. */
    suspend fun unicast(sessionId: String, payload: WebSocketPayload): Boolean

    /** Text convenience for [broadcast]. */
    suspend fun broadcast(text: String): Int = broadcast(WebSocketPayload.Text(text))

    /** Text convenience for [broadcastToUser]. */
    suspend fun broadcastToUser(userId: String, text: String): Int =
        broadcastToUser(userId, WebSocketPayload.Text(text))

    /** Text convenience for [broadcastToTenant]. */
    suspend fun broadcastToTenant(tenantId: String, text: String): Int =
        broadcastToTenant(tenantId, WebSocketPayload.Text(text))

    /** Text convenience for [unicast]. */
    suspend fun unicast(sessionId: String, text: String): Boolean =
        unicast(sessionId, WebSocketPayload.Text(text))

    /** Binary convenience for [broadcast]. */
    suspend fun broadcast(bytes: ByteArray): Int = broadcast(WebSocketPayload.Binary(bytes))

    /** Binary convenience for [broadcastToUser]. */
    suspend fun broadcastToUser(userId: String, bytes: ByteArray): Int =
        broadcastToUser(userId, WebSocketPayload.Binary(bytes))

    /** Binary convenience for [broadcastToTenant]. */
    suspend fun broadcastToTenant(tenantId: String, bytes: ByteArray): Int =
        broadcastToTenant(tenantId, WebSocketPayload.Binary(bytes))

    /** Binary convenience for [unicast]. */
    suspend fun unicast(sessionId: String, bytes: ByteArray): Boolean =
        unicast(sessionId, WebSocketPayload.Binary(bytes))
}
