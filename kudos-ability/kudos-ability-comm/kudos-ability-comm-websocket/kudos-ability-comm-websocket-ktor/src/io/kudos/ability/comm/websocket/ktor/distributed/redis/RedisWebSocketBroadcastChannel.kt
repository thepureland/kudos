package io.kudos.ability.comm.websocket.ktor.distributed.redis

import io.kudos.ability.comm.websocket.ktor.distributed.IWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope
import io.kudos.base.logger.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Redis pub/sub backed [IWebSocketBroadcastChannel].
 *
 * Publishes [WebSocketBroadcastEnvelope]s onto a Redis channel via [RedisTemplate.convertAndSend];
 * subscribes by attaching itself as a [MessageListener] to the supplied [container] on construction.
 * Every subscriber registered through [subscribe] is invoked for every inbound envelope, including
 * envelopes this very process published — self-echo filtering is the subscriber's responsibility
 * (see [io.kudos.ability.comm.websocket.ktor.distributed.DistributedWebSocketBroadcaster] for the
 * canonical handler that does this).
 *
 * Serialization piggybacks on whatever value serializer [redisTemplate] is configured with. The
 * kudos default (Jackson) handles [WebSocketBroadcastEnvelope] as a plain data class; the envelope
 * is also [java.io.Serializable] so a `JdkSerializationRedisSerializer` works as a fallback. If a
 * specialized serializer (e.g. kotlinx.serialization) is in play, register the envelope class as
 * needed before constructing this channel.
 *
 * Deserialization is wrapped in a try/catch: a malformed payload is logged at ERROR but does not
 * propagate, otherwise Spring's listener thread would die and silently drop every subsequent message
 * on this node. The same is true for handler exceptions — they are isolated per-handler.
 *
 * Handler dispatch runs on a private [SupervisorJob] + [Dispatchers.Default] scope so the listener
 * thread is released as soon as it has scheduled the work; a slow / suspending handler does not
 * block the next Redis delivery.
 *
 * Wiring example:
 * ```kotlin
 * val nodeId = UUID.randomUUID().toString()
 * val channel = RedisWebSocketBroadcastChannel(
 *     redisTemplate = redisTemplates.defaultRedisTemplate,
 *     container = redisMessageListenerContainer,
 *     redisChannel = "kudos:ws:broadcast",
 * )
 * val broadcaster = DistributedWebSocketBroadcaster(
 *     local = WebSocketBroadcaster(registry),
 *     channel = channel,
 *     nodeId = nodeId,
 * )
 * ```
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RedisWebSocketBroadcastChannel(
    private val redisTemplate: RedisTemplate<*, *>,
    container: RedisMessageListenerContainer,
    /** Redis pub/sub channel name. Different deployments must agree on the same name. */
    private val redisChannel: String,
) : IWebSocketBroadcastChannel, MessageListener {

    private val log = LogFactory.getLog(this::class)

    /** Dispatches inbound deliveries; one supervisor scope keeps a buggy handler from killing siblings. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val handlers = CopyOnWriteArrayList<suspend (WebSocketBroadcastEnvelope) -> Unit>()

    init {
        container.addMessageListener(this, ChannelTopic(redisChannel))
    }

    /**
     * Publishes the envelope onto the configured Redis channel. Offloaded to [Dispatchers.IO] so that
     * the (blocking) `convertAndSend` call never holds the calling event-loop coroutine; the suspend
     * point is short — Redis ack on local datacenter is sub-ms typical.
     */
    override suspend fun publish(envelope: WebSocketBroadcastEnvelope) {
        withContext(Dispatchers.IO) {
            redisTemplate.convertAndSend(redisChannel, envelope)
        }
    }

    override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit) {
        handlers += handler
    }

    /**
     * Spring [MessageListener] entry point. Deserializes via [redisTemplate]'s value serializer and
     * dispatches every registered handler on the internal coroutine scope. Failures stay local.
     */
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val envelope: WebSocketBroadcastEnvelope = try {
            redisTemplate.valueSerializer?.deserialize(message.body) as? WebSocketBroadcastEnvelope
                ?: run {
                    // A null deserializer or a type mismatch usually means misconfigured RedisTemplate.
                    // Log and drop — silently swallowing would leave operators chasing phantom "missed
                    // broadcasts" for weeks.
                    log.warn("Redis WebSocket broadcast envelope deserialized to null or wrong type; check redisTemplate.valueSerializer")
                    return
                }
        } catch (e: Exception) {
            log.error(e, "Failed to deserialize Redis WebSocket broadcast envelope; this node will miss the broadcast")
            return
        }

        for (handler in handlers) {
            scope.launch {
                runCatching { handler(envelope) }
                    .onFailure { log.error(it, "Redis WebSocket broadcast handler failed nodeId={0} targetType={1}", envelope.nodeId, envelope.targetType) }
            }
        }
    }
}
