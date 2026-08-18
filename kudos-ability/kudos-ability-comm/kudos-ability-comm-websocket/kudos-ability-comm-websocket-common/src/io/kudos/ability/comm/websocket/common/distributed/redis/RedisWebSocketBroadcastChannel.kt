package io.kudos.ability.comm.websocket.common.distributed.redis

import io.kudos.ability.comm.websocket.common.distributed.IWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.common.distributed.WebSocketBroadcastEnvelope
import io.kudos.ability.comm.websocket.common.distributed.WebSocketBroadcastSubscription
import io.kudos.base.logger.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
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
 * (see [io.kudos.ability.comm.websocket.common.distributed.DistributedWebSocketBroadcaster] for the
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
 * ## Ordering
 *
 * Inbound envelopes are handed to a bounded [Channel] drained by a single consumer coroutine, rather
 * than each being dispatched on its own coroutine. Redis delivers messages to a listener in publish
 * order, and spawning a coroutine per message throws that order away — two chat messages published a
 * millisecond apart could be handed to the handler in either order, which users notice. The single
 * consumer preserves order while still releasing Spring's listener thread immediately.
 *
 * The tradeoff is that a slow handler delays the messages behind it; that is inherent to ordered
 * delivery. If the buffer fills (a handler stuck for [inboundBufferCapacity] messages), further
 * envelopes are dropped with a WARN — dropping loudly beats blocking the Redis listener thread,
 * which would stall every other listener sharing the container.
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
    private val container: RedisMessageListenerContainer,
    /** Redis pub/sub channel name. Different deployments must agree on the same name. */
    private val redisChannel: String,
    /** How many inbound envelopes may queue while handlers are busy before delivery starts dropping. */
    private val inboundBufferCapacity: Int = DEFAULT_INBOUND_BUFFER_CAPACITY,
) : IWebSocketBroadcastChannel, MessageListener, AutoCloseable {

    private val log = LogFactory.getLog(this::class)

    /** Dispatches inbound deliveries; one supervisor scope keeps a buggy handler from killing siblings. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val handlers = CopyOnWriteArrayList<suspend (WebSocketBroadcastEnvelope) -> Unit>()

    private val topic = ChannelTopic(redisChannel)

    /** Hand-off from Spring's listener thread to the ordered consumer below. */
    private val inbound = Channel<WebSocketBroadcastEnvelope>(capacity = inboundBufferCapacity)

    init {
        scope.launch {
            for (envelope in inbound) {
                for (handler in handlers) {
                    runCatching { handler(envelope) }
                        .onFailure { log.error(it, "Redis WebSocket broadcast handler failed nodeId={0} targetType={1}", envelope.nodeId, envelope.targetType) }
                }
            }
        }
        container.addMessageListener(this, topic)
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

    override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit): WebSocketBroadcastSubscription {
        handlers += handler
        return WebSocketBroadcastSubscription { handlers.remove(handler) }
    }

    /**
     * Spring [MessageListener] entry point. Deserializes via [redisTemplate]'s value serializer and
     * queues the envelope for the ordered consumer. Failures stay local; the listener thread is
     * never blocked and never allowed to die.
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

        if (inbound.trySend(envelope).isFailure) {
            log.warn("Redis WebSocket inbound buffer full (capacity={0}); dropping envelope from nodeId={1} targetType={2}. A handler is too slow or stuck.",
                inboundBufferCapacity, envelope.nodeId, envelope.targetType)
        }
    }

    /**
     * Detaches from the listener container and stops the consumer. Spring calls this automatically
     * for beans implementing [AutoCloseable]; tests and manual wiring must call it themselves, or the
     * container keeps a reference to a dead channel and its consumer coroutine leaks.
     */
    override fun close() {
        runCatching { container.removeMessageListener(this, topic) }
            .onFailure { log.warn("Removing Redis WebSocket listener failed channel={0} cause={1}", redisChannel, it.message) }
        inbound.close()
        scope.cancel()
    }

    companion object {
        /** Deep enough to ride out a GC pause or a slow handler, shallow enough to fail loudly instead of hoarding memory. */
        const val DEFAULT_INBOUND_BUFFER_CAPACITY: Int = 1024
    }
}
