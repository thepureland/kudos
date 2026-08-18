package io.kudos.ability.comm.websocket.common.handler

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef

/**
 * Base class for handler decorators — the per-message interception seam.
 *
 * There is deliberately no separate "message interceptor" SPI: an [IKudosWebSocketHandler] that
 * wraps another one already *is* an interceptor, and a strictly more capable one than the
 * `preHandle` / `postHandle` shape of e.g. Spring's `HandlerInterceptor`. A decorator can run code
 * before **and** after the delegate in the same stack frame, so timing, `try`/`finally`, opening a
 * tracing span, or establishing a coroutine context around the delegate all work; it can also
 * short-circuit by simply not calling through.
 *
 * This class exists only so a decorator that cares about one hook does not have to hand-write
 * delegation for the other three. Extend it and override what you need:
 *
 * ```kotlin
 * class TimingHandler(next: IKudosWebSocketHandler) : WebSocketHandlerDecorator(next) {
 *     override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
 *         val startedAt = System.nanoTime()
 *         try { super.onText(session, text) } finally { record(System.nanoTime() - startedAt) }
 *     }
 * }
 * ```
 *
 * Assemble chains with [wrappedBy].
 *
 * @param delegate the next handler in the chain
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
abstract class WebSocketHandlerDecorator(
    protected val delegate: IKudosWebSocketHandler,
) : IKudosWebSocketHandler {

    override suspend fun onConnect(session: KudosWebSocketSessionRef) = delegate.onConnect(session)

    override suspend fun onText(session: KudosWebSocketSessionRef, text: String) = delegate.onText(session, text)

    override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) = delegate.onBinary(session, bytes)

    override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) =
        delegate.onDisconnect(session, cause)
}

/**
 * Wraps this handler in [factories], **outermost first**, and returns the head of the chain.
 *
 * Written as a fold so call sites read in execution order instead of the inside-out nesting that
 * hand-written composition forces (`Tracing(RateLimit(chatHandler))` reads backwards and gets worse
 * with every layer):
 *
 * ```kotlin
 * val handler = chatHandler.wrappedBy(::TracingHandler, ::RateLimitingWebSocketHandler)
 * // a message passes through Tracing, then RateLimit, then chatHandler
 * ```
 *
 * Note that the lambdas go **inside the parentheses**: Kotlin does not accept trailing-lambda syntax
 * for a `vararg` parameter, so `handler.wrappedBy { ... }` will not compile — write
 * `handler.wrappedBy({ ... })`.
 *
 * @param factories each takes the next handler and returns the wrapping one
 * @return the outermost handler, or this one when [factories] is empty
 */
fun IKudosWebSocketHandler.wrappedBy(
    vararg factories: (IKudosWebSocketHandler) -> IKudosWebSocketHandler,
): IKudosWebSocketHandler = factories.foldRight(this) { factory, next -> factory(next) }

/**
 * [wrappedBy] for a chain assembled at runtime — Spring beans, say:
 * `handler.wrappedBy(provider.orderedStream().toList())`. `ObjectProvider.orderedStream()` already
 * honors `@Order` / `Ordered`, so this module needs no ordering machinery of its own.
 */
fun IKudosWebSocketHandler.wrappedBy(
    factories: List<(IKudosWebSocketHandler) -> IKudosWebSocketHandler>,
): IKudosWebSocketHandler = factories.foldRight(this) { factory, next -> factory(next) }
