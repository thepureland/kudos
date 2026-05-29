package io.kudos.ability.comm.websocket.ktor.distributed

import io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end tests for [DistributedWebSocketBroadcaster] using an [InMemoryBroadcastChannel] shared by
 * two simulated nodes. Each "node" has its own registry + local broadcaster + distributed decorator;
 * sessions live on one side or the other. The tests assert that:
 *  - cross-node delivery reaches sessions on the *other* node (the Redis-bridged behavior the kudos
 *    README marks as "leave to business");
 *  - self-echo filtering does not double-deliver on the originating node;
 *  - exceptions from `local` during inbound dispatch do not terminate the channel subscription.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DistributedWebSocketBroadcasterTest {

    private fun newCluster(channel: InMemoryBroadcastChannel = InMemoryBroadcastChannel()): Cluster {
        val regA = KudosWebSocketRegistry()
        val regB = KudosWebSocketRegistry()
        val distA = DistributedWebSocketBroadcaster(WebSocketBroadcaster(regA), channel, nodeId = "node-A")
        val distB = DistributedWebSocketBroadcaster(WebSocketBroadcaster(regB), channel, nodeId = "node-B")
        return Cluster(regA, regB, distA, distB, channel)
    }

    @Test
    fun broadcast_reachesSessionsOnRemoteNode() = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A", tenantId = "t-1").also(cluster.regA::register)
        val sB = RecordingSession("s-B", userId = "u-B", tenantId = "t-1").also(cluster.regB::register)

        val locallyReached = cluster.distA.broadcast("hello-all")

        assertEquals(1, locallyReached, "broadcast() returns the count of *local* sessions reached")
        assertEquals(listOf("hello-all"), sA.received, "Local session on A receives the message directly")
        assertEquals(listOf("hello-all"), sB.received, "Remote session on B receives the message via the channel")
    }

    @Test
    fun broadcastToUser_isRoutedAcrossNodes() = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A").also(cluster.regA::register)
        val sB = RecordingSession("s-B", userId = "u-A").also(cluster.regB::register)
        val sBother = RecordingSession("s-B-other", userId = "u-other").also(cluster.regB::register)

        cluster.distA.broadcastToUser("u-A", "hi-u-A")

        assertEquals(listOf("hi-u-A"), sA.received)
        assertEquals(listOf("hi-u-A"), sB.received)
        assertTrue(sBother.received.isEmpty(), "Sessions for a different userId on the remote node are not reached")
    }

    @Test
    fun broadcastToTenant_isRoutedAcrossNodes() = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", tenantId = "t-1").also(cluster.regA::register)
        val sB = RecordingSession("s-B", tenantId = "t-1").also(cluster.regB::register)
        val sBother = RecordingSession("s-B-other", tenantId = "t-2").also(cluster.regB::register)

        cluster.distA.broadcastToTenant("t-1", "tenant-1-only")

        assertEquals(listOf("tenant-1-only"), sA.received)
        assertEquals(listOf("tenant-1-only"), sB.received)
        assertTrue(sBother.received.isEmpty(), "Sessions for a different tenant on the remote node are not reached")
    }

    @Test
    fun unicast_remoteSession_returnsLocalFalseButRemoteDelivers() = runBlocking {
        val cluster = newCluster()
        val sB = RecordingSession("s-B").also(cluster.regB::register)

        // A unicasts to a sessionId that is registered on B, not A.
        val localOk = cluster.distA.unicast("s-B", "ping")

        assertFalse(localOk, "A's local registry does not hold s-B → unicast returns local-false")
        assertEquals(listOf("ping"), sB.received, "B still receives the message via channel-backed delivery")
    }

    @Test
    fun selfEcho_doesNotDoubleDeliverOnOriginatingNode() = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A").also(cluster.regA::register)

        cluster.distA.broadcastToUser("u-A", "once")

        assertEquals(
            listOf("once"), sA.received,
            "Originating-node session must receive exactly one delivery — local fan-out only, " +
                "the pub/sub self-delivery is dropped by the nodeId filter.",
        )
    }

    @Test
    fun inboundDispatch_failureDoesNotTerminateSubscription() = runBlocking {
        val channel = InMemoryBroadcastChannel()
        val cluster = newCluster(channel)
        val sB = RecordingSession("s-B", userId = "u-A").also(cluster.regB::register)
        val sBcrashing = CrashingSession("s-B-crash", userId = "u-A").also(cluster.regB::register)

        // First inbound delivery causes one session to throw; subsequent calls must still work.
        cluster.distA.broadcastToUser("u-A", "first")
        cluster.distA.broadcastToUser("u-A", "second")

        assertEquals(listOf("first", "second"), sB.received,
            "Subsequent broadcasts must still reach the healthy remote session after a sibling session threw.")
        assertEquals(2, sBcrashing.attempts, "The crashing session is still attempted on every delivery (not blacklisted).")
    }

    private data class Cluster(
        val regA: KudosWebSocketRegistry,
        val regB: KudosWebSocketRegistry,
        val distA: DistributedWebSocketBroadcaster,
        val distB: DistributedWebSocketBroadcaster,
        val channel: InMemoryBroadcastChannel,
    )

    /** Captures `sendText` payloads in order; thread-safe under the concurrent broadcaster. */
    private class RecordingSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        val received: MutableList<String> = CopyOnWriteArrayList()
        override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()
        override suspend fun sendText(text: String) { received += text }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: CloseReason) {}
    }

    /** Always throws on send — used to assert one bad session does not poison the channel subscription. */
    private class CrashingSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        var attempts: Int = 0
            private set
        override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {
            attempts++
            error("boom")
        }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: CloseReason) {}
    }
}
