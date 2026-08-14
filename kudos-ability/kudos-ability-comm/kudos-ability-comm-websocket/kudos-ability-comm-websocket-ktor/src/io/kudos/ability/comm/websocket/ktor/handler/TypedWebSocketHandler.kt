package io.kudos.ability.comm.websocket.ktor.handler

import io.kudos.ability.comm.websocket.ktor.codec.IWebSocketMessageEncoder
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.kudos.base.logger.LogFactory

/**
 * [IKudosWebSocketHandler] that decodes each inbound text message into [T] before handing it to
 * business code, using an [IWebSocketMessageEncoder].
 *
 * This is the inbound half of the codec SPI: without it, every handler re-implements the same
 * "parse the frame, and decide what to do when the client sends garbage" boilerplate. The
 * outbound half needs no scaffolding — `broadcaster.broadcast(encoder.encode(msg))` is already
 * one line.
 *
 * A decode failure must never kill the connection by default: a single malformed message from one
 * client is a client bug, not a reason to drop a session that may be carrying valid traffic. It is
 * routed to [onDecodeFailure], which logs and continues; override to reply with an error message
 * or to close the session on repeated abuse.
 *
 * ```kotlin
 * class ChatHandler(encoder: IWebSocketMessageEncoder) : TypedWebSocketHandler<ChatMessage>(encoder, ChatMessage::class.java) {
 *     override suspend fun onMessage(session: KudosWebSocketSessionRef, message: ChatMessage) {
 *         broadcaster.broadcastToTenant(session.tenantId ?: "default", encoder.encode(message.reply()))
 *     }
 * }
 * ```
 *
 * @param T the decoded business message type
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
abstract class TypedWebSocketHandler<T : Any>(
    private val encoder: IWebSocketMessageEncoder,
    private val messageType: Class<T>,
) : IKudosWebSocketHandler {

    private val log = LogFactory.getLog(this::class)

    /** Final: subclasses hook in at [onMessage], so the decode-and-guard step cannot be skipped by accident. */
    final override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
        val message = try {
            encoder.decode(text, messageType)
        } catch (t: Throwable) {
            onDecodeFailure(session, text, t)
            return
        }
        onMessage(session, message)
    }

    /** Called once per successfully decoded message. */
    abstract suspend fun onMessage(session: KudosWebSocketSessionRef, message: T)

    /** Called when [text] could not be decoded into [T]. Default: log at WARN and drop the message. */
    protected open suspend fun onDecodeFailure(session: KudosWebSocketSessionRef, text: String, cause: Throwable) {
        log.warn("Failed to decode WebSocket message into {0} sessionId={1} cause={2}",
            messageType.name, session.sessionId, cause.message)
    }
}

/**
 * Builds a [TypedWebSocketHandler] for [T] from a lambda, so simple cases do not need a named
 * subclass just to pass `T::class.java`.
 *
 * ```kotlin
 * val handler = typedWebSocketHandler<ChatMessage>(encoder) { session, message -> ... }
 * ```
 */
inline fun <reified T : Any> typedWebSocketHandler(
    encoder: IWebSocketMessageEncoder,
    crossinline block: suspend (KudosWebSocketSessionRef, T) -> Unit,
): TypedWebSocketHandler<T> = object : TypedWebSocketHandler<T>(encoder, T::class.java) {
    override suspend fun onMessage(session: KudosWebSocketSessionRef, message: T) = block(session, message)
}
