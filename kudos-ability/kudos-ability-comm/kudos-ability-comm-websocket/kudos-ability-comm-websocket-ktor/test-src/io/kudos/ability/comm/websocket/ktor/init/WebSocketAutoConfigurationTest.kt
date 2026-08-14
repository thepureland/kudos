package io.kudos.ability.comm.websocket.ktor.init

import io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.DistributedWebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.IWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.ktor.distributed.InMemoryBroadcastChannel
import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.ObjectProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [WebSocketAutoConfiguration]'s bean factory methods, exercised directly (no Spring
 * container) in the style of the sibling `EmailAutoConfigurationTest`.
 *
 * The behavior worth pinning down is the local-vs-distributed decision and the fact that configured
 * properties actually reach the objects built here — a properties class that is bound but never
 * passed through is a bug the container would happily start with.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketAutoConfigurationTest {

    private val configuration = WebSocketAutoConfiguration()

    @Test
    fun defaults_areSingleInstanceFriendly() {
        val properties = configuration.webSocketProperties()

        assertFalse(properties.distributed.enabled, "Distributed broadcasting must be opt-in — a single instance should not need Redis")
        assertEquals("", properties.nodeId, "A blank nodeId means 'generate one at startup'")
        assertEquals("kudos:ws:broadcast", properties.distributed.redisChannel)
        assertTrue(properties.broadcast.closeOnSendTimeout)
    }

    @Test
    fun registryBean_isAFreshRegistry() {
        val registry = configuration.kudosWebSocketRegistry()

        assertEquals(0, registry.size)
    }

    @Test
    fun withoutABroadcastChannel_theBroadcasterIsProcessLocal() {
        val broadcaster = configuration.webSocketBroadcaster(
            registry = KudosWebSocketRegistry(),
            properties = WebSocketProperties(),
            channelProvider = absentProvider(),
        )

        assertIs<WebSocketBroadcaster>(broadcaster, "No channel bean → no reason to pay for cross-process fan-out")
    }

    @Test
    fun withABroadcastChannel_theBroadcasterGoesDistributed_andCarriesTheConfiguredNodeId() = runBlocking {
        val channel = InMemoryBroadcastChannel()
        val published = CopyOnWriteArrayList<WebSocketBroadcastEnvelope>()
        channel.subscribe { published += it }

        val broadcaster = configuration.webSocketBroadcaster(
            registry = KudosWebSocketRegistry(),
            properties = WebSocketProperties(nodeId = "node-from-config"),
            channelProvider = providerOf(channel),
        )
        assertIs<DistributedWebSocketBroadcaster>(broadcaster)

        broadcaster.broadcast("hi")

        assertEquals(listOf("node-from-config"), published.map { it.nodeId },
            "The configured nodeId must reach the envelope, or self-echo filtering breaks")
    }

    @Test
    fun blankNodeId_isReplacedByAGeneratedOne() = runBlocking {
        val channel = InMemoryBroadcastChannel()
        val published = CopyOnWriteArrayList<WebSocketBroadcastEnvelope>()
        channel.subscribe { published += it }

        val broadcaster = configuration.webSocketBroadcaster(
            registry = KudosWebSocketRegistry(),
            properties = WebSocketProperties(nodeId = ""),
            channelProvider = providerOf(channel),
        )
        broadcaster.broadcast("hi")

        val nodeId = published.single().nodeId
        assertTrue(nodeId.isNotBlank(), "A blank configured nodeId must be replaced, otherwise every node shares an identity")
    }

    @Test
    fun broadcastProperties_areAppliedToTheBroadcaster() = runBlocking {
        val registry = KudosWebSocketRegistry()
        val stuck = StuckSession("stuck").also(registry::register)
        val properties = WebSocketProperties(
            broadcast = WebSocketProperties.Broadcast(sendTimeoutMillis = 50, closeOnSendTimeout = true),
        )

        val broadcaster = configuration.webSocketBroadcaster(registry, properties, absentProvider())

        // Would hang forever if sendTimeoutMillis were parsed but never passed to the broadcaster.
        assertFalse(broadcaster.unicast("stuck", "msg"))
        assertTrue(stuck.closed, "closeOnSendTimeout = true must reach the broadcaster too")
    }

    @Test
    fun getComponentName() {
        assertEquals("kudos-ability-comm-websocket-ktor", configuration.getComponentName())
    }

    /** Mirrors Spring's behavior for an absent bean: `getObject` throws, so `getIfAvailable` yields null. */
    private fun absentProvider() = object : ObjectProvider<IWebSocketBroadcastChannel> {
        override fun getObject(): IWebSocketBroadcastChannel =
            throw NoSuchBeanDefinitionException(IWebSocketBroadcastChannel::class.java)
    }

    private fun providerOf(channel: IWebSocketBroadcastChannel) = object : ObjectProvider<IWebSocketBroadcastChannel> {
        override fun getObject(): IWebSocketBroadcastChannel = channel
    }

    /** Never completes a send — used to observe the configured send deadline. */
    private class StuckSession(override val sessionId: String) : KudosWebSocketSessionRef {
        override val userId: String? = null
        override val tenantId: String? = null
        @Volatile var closed: Boolean = false
            private set
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) { delay(Long.MAX_VALUE) }
        override suspend fun sendBinary(bytes: ByteArray) { delay(Long.MAX_VALUE) }
        override suspend fun close(reason: CloseReason) { closed = true }
    }
}
