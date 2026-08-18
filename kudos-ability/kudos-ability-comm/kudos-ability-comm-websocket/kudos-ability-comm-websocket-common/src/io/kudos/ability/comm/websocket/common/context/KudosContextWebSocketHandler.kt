package io.kudos.ability.comm.websocket.common.context

import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.handler.WebSocketHandlerDecorator
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.context.core.KudosContext

/**
 * Establishes a [KudosContext] around every handler hook, so business code reached from a WebSocket
 * message can use the same context machinery an HTTP request would.
 *
 * Without this, a session carries `userId` / `tenantId` that nothing downstream can see: kudos'
 * tenant-aware cache keys, ktorm's `currentDatabase()` routing and audit-field population all read
 * [io.kudos.context.core.KudosContextHolder], which no WebSocket code path ever populates.
 *
 * The context is bound through [withKudosContextBound], i.e. via a
 * [KudosContextThreadElement] as well as a [io.kudos.context.core.KudosContextElement] — see that
 * class for why a plain `set` / `finally clear` pair does not work inside a coroutine.
 *
 * ## Opt-in, and per message
 *
 * This is a decorator rather than something the route does automatically: binding a context changes
 * what every downstream read sees, which is not a default worth imposing on connections that never
 * touch tenant-aware code.
 *
 * [contextFactory] is invoked **per hook invocation**, not once per connection, so each message gets
 * its own [KudosContext] — a fresh `traceKey` per message is what makes tracing useful, and nothing
 * mutable is shared between messages. Override it to fill in what this module cannot: notably
 * [KudosContext.user], which needs an `IIdEntity` that a `userId` string alone cannot produce.
 *
 * ```kotlin
 * val handler = chatHandler.wrappedBy({ next ->
 *     KudosContextWebSocketHandler(next) { session ->
 *         KudosContext().apply {
 *             tenantId = session.tenantId
 *             user = userService.loadById(session.userId)
 *             traceKey = RandomStringKit.uuid()
 *         }
 *     }
 * })
 * ```
 *
 * @param delegate the next handler in the chain
 * @param contextFactory builds the context for one hook invocation
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class KudosContextWebSocketHandler(
    delegate: IKudosWebSocketHandler,
    private val contextFactory: (KudosWebSocketSessionRef) -> KudosContext = ::defaultWebSocketContext,
) : WebSocketHandlerDecorator(delegate) {

    override suspend fun onConnect(session: KudosWebSocketSessionRef) =
        withKudosContextBound(contextFactory(session)) { delegate.onConnect(session) }

    override suspend fun onText(session: KudosWebSocketSessionRef, text: String) =
        withKudosContextBound(contextFactory(session)) { delegate.onText(session, text) }

    override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) =
        withKudosContextBound(contextFactory(session)) { delegate.onBinary(session, bytes) }

    override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) =
        withKudosContextBound(contextFactory(session)) { delegate.onDisconnect(session, cause) }
}

/**
 * Default [KudosContext] for a WebSocket hook: the session's `tenantId` plus a fresh `traceKey`.
 *
 * [KudosContext.user] is deliberately left null — it is an `IIdEntity<String>`, and a session only
 * carries a `userId` string; fabricating a stand-in entity here would put a half-populated object
 * where downstream code expects a real one. Supply a factory that loads the user when business code
 * needs it.
 */
fun defaultWebSocketContext(session: KudosWebSocketSessionRef): KudosContext = KudosContext().apply {
    tenantId = session.tenantId
    traceKey = RandomStringKit.uuid()
}
