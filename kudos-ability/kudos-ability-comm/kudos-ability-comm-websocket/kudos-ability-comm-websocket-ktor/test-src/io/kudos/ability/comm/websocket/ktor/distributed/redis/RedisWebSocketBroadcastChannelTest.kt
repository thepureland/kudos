package io.kudos.ability.comm.websocket.ktor.distributed.redis

import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope
import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope.TargetType
import kotlinx.coroutines.runBlocking
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.data.redis.connection.DefaultMessage
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.Topic
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisWebSocketBroadcastChannel] with mocked / connection-less Redis collaborators
 * (no real Redis server):
 * - construction registers itself as a [org.springframework.data.redis.connection.MessageListener]
 *   on the supplied container for exactly the configured channel topic;
 * - publish delegates to `RedisTemplate.convertAndSend` with the channel name and envelope;
 * - onMessage happy path: JDK-serialized envelope is decoded and fanned out to every subscribed
 *   handler (asynchronously, on the internal scope);
 * - onMessage degenerate paths: null valueSerializer, wrong deserialized type, and a serializer
 *   exception are each logged and dropped without invoking handlers or propagating;
 * - a throwing handler does not prevent sibling handlers from receiving the envelope.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RedisWebSocketBroadcastChannelTest {

    private val channelName = "kudos:ws:broadcast-test"

    /** Connection-less RedisTemplate whose valueSerializer is the JDK one (the documented fallback). */
    private fun jdkTemplate(): RedisTemplate<Any, Any> = RedisTemplate<Any, Any>().apply {
        valueSerializer = JdkSerializationRedisSerializer()
    }

    private fun jdkBytes(value: Any): ByteArray = JdkSerializationRedisSerializer().serialize(value)!!

    private fun envelope(text: String = "hi") =
        WebSocketBroadcastEnvelope(nodeId = "node-1", targetType = TargetType.ALL, targetId = null, text = text)

    @Test
    fun construction_registersListenerOnContainerForConfiguredTopic() {
        val container = mock(RedisMessageListenerContainer::class.java)

        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)

        val listenerCaptor = ArgumentCaptor.forClass(org.springframework.data.redis.connection.MessageListener::class.java)
        val topicCaptor = ArgumentCaptor.forClass(Topic::class.java)
        verify(container).addMessageListener(listenerCaptor.capture(), topicCaptor.capture())
        assertSame(channel, listenerCaptor.value, "The channel itself must be the registered listener")
        assertTrue(topicCaptor.value is ChannelTopic)
        assertEquals(channelName, topicCaptor.value.topic)
    }

    // runBlocking<Unit> is load-bearing: convertAndSend returns Long, so without the explicit type
    // argument this @Test method would return a value and JUnit 5 silently refuses to execute it
    // ("must not return a value" warning) — which previously hid this test from every run.
    @Test
    fun publish_delegatesToConvertAndSend() = runBlocking<Unit> {
        @Suppress("UNCHECKED_CAST")
        val template = mock(RedisTemplate::class.java) as RedisTemplate<Any, Any>
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(template, container, channelName)
        val env = envelope("publish-me")

        channel.publish(env)

        verify(template).convertAndSend(channelName, env)
    }

    @Test
    fun onMessage_validEnvelope_isFannedOutToAllHandlers() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val received = ConcurrentLinkedQueue<WebSocketBroadcastEnvelope>()
        val latch = CountDownLatch(2)
        repeat(2) {
            channel.subscribe { env ->
                received += env
                latch.countDown()
            }
        }
        val env = envelope("fan-out 中文🚀")

        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(env)), null)

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Both handlers must receive the envelope")
        assertEquals(listOf(env, env), received.toList())
    }

    @Test
    fun onMessage_nullValueSerializer_isDroppedWithoutInvokingHandlers() {
        val container = mock(RedisMessageListenerContainer::class.java)
        // Fresh RedisTemplate without afterPropertiesSet: valueSerializer stays null.
        val channel = RedisWebSocketBroadcastChannel(RedisTemplate<Any, Any>(), container, channelName)
        val latch = CountDownLatch(1)
        channel.subscribe { latch.countDown() }

        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope())), null)

        assertFalse(latch.await(150, TimeUnit.MILLISECONDS), "No handler may be invoked when the serializer is null")
    }

    @Test
    fun onMessage_wrongPayloadType_isDroppedWithoutInvokingHandlers() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val latch = CountDownLatch(1)
        channel.subscribe { latch.countDown() }

        // A serializable payload that is not a WebSocketBroadcastEnvelope → `as?` yields null → drop.
        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes("just a string")), null)

        assertFalse(latch.await(150, TimeUnit.MILLISECONDS), "Wrong-type payloads must be dropped, not dispatched")
    }

    @Test
    fun onMessage_deserializationFailure_isCaughtAndDropped() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val latch = CountDownLatch(1)
        channel.subscribe { latch.countDown() }

        // Garbage bytes blow up the JDK deserializer; onMessage must swallow (listener thread survival).
        channel.onMessage(DefaultMessage(channelName.toByteArray(), byteArrayOf(1, 2, 3)), null)

        assertFalse(latch.await(150, TimeUnit.MILLISECONDS), "Undeserializable payloads must be dropped, not dispatched")
    }

    @Test
    fun onMessage_throwingHandler_doesNotPreventSiblingDelivery() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val crashed = CountDownLatch(1)
        val delivered = CountDownLatch(1)
        channel.subscribe {
            crashed.countDown()
            error("handler exploded")
        }
        channel.subscribe { delivered.countDown() }

        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope())), null)

        assertTrue(crashed.await(5, TimeUnit.SECONDS), "The crashing handler is still attempted")
        assertTrue(delivered.await(5, TimeUnit.SECONDS), "The healthy handler must receive the envelope despite the sibling crash")
    }

    @Test
    fun onMessage_deliversInPublishOrder() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val texts = ConcurrentLinkedQueue<String>()
        val count = 50
        val latch = CountDownLatch(count)
        channel.subscribe { env ->
            texts += env.text
            latch.countDown()
        }

        // Redis hands the listener messages in publish order; dispatching each on its own coroutine
        // would shuffle them, which is visible to users in any chat-like feature.
        repeat(count) { i ->
            channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope("msg-$i"))), null)
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All envelopes must be delivered")
        assertEquals((0 until count).map { "msg-$it" }, texts.toList(), "Delivery order must match publish order")
    }

    @Test
    fun close_removesTheListenerAndStopsDelivery() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val latch = CountDownLatch(1)
        channel.subscribe { latch.countDown() }

        channel.close()

        val listenerCaptor = ArgumentCaptor.forClass(org.springframework.data.redis.connection.MessageListener::class.java)
        val topicCaptor = ArgumentCaptor.forClass(Topic::class.java)
        verify(container).removeMessageListener(listenerCaptor.capture(), topicCaptor.capture())
        assertSame(channel, listenerCaptor.value, "The channel must detach the same listener it registered")
        assertEquals(channelName, topicCaptor.value.topic)

        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope())), null)
        assertFalse(latch.await(150, TimeUnit.MILLISECONDS), "A closed channel must not keep dispatching")
    }

    @Test
    fun subscription_close_detachesOnlyThatHandler() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val detached = CountDownLatch(1)
        val kept = CountDownLatch(1)
        val subscription = channel.subscribe { detached.countDown() }
        channel.subscribe { kept.countDown() }

        subscription.close()
        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope())), null)

        assertTrue(kept.await(5, TimeUnit.SECONDS), "The remaining handler must still receive envelopes")
        assertFalse(detached.await(150, TimeUnit.MILLISECONDS), "A closed subscription must stop receiving")
    }

    @Test
    fun onMessage_afterHandlerCrash_subsequentMessagesStillDispatch() {
        val container = mock(RedisMessageListenerContainer::class.java)
        val channel = RedisWebSocketBroadcastChannel(jdkTemplate(), container, channelName)
        val texts = ConcurrentLinkedQueue<String>()
        val latch = CountDownLatch(2)
        channel.subscribe { env ->
            texts += env.text
            latch.countDown()
            error("always crashes after recording")
        }

        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope("first"))), null)
        channel.onMessage(DefaultMessage(channelName.toByteArray(), jdkBytes(envelope("second"))), null)

        assertTrue(latch.await(5, TimeUnit.SECONDS), "The supervisor scope must keep dispatching after a handler failure")
        assertEquals(setOf("first", "second"), texts.toSet())
    }
}
