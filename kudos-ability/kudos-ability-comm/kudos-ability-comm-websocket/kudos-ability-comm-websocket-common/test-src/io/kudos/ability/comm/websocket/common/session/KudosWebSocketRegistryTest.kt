package io.kudos.ability.comm.websocket.common.session

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for the three [KudosWebSocketRegistry] indexes (id / userId / tenantId) — register / unregister.
 *
 * Uses [StubSessionRef] instead of a real [KudosWebSocketSession] — the registry only reads metadata fields,
 * so the stub avoids the Ktor dependency.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosWebSocketRegistryTest {

    @Test
    fun register_addsToAllIndexesAndIncrementsSize() {
        val r = KudosWebSocketRegistry()
        val s = StubSessionRef("s1", userId = "u1", tenantId = "t1")

        r.register(s)

        assertEquals(1, r.size)
        assertSame(s, r.findById("s1"))
        assertEquals(listOf(s), r.findByUserId("u1"))
        assertEquals(listOf(s), r.findByTenantId("t1"))
    }

    @Test
    fun register_withNullUserAndTenant_onlyIndexesById() {
        val r = KudosWebSocketRegistry()
        val s = StubSessionRef("s1", userId = null, tenantId = null)

        r.register(s)

        assertSame(s, r.findById("s1"))
        // Anonymous sessions should not be added to the user / tenant indexes — empty-key lookups find nothing
        assertEquals(emptyList(), r.findByUserId(""))
        assertEquals(emptyList(), r.findByTenantId(""))
    }

    @Test
    fun register_multipleSessionsForSameUser_allListed() {
        val r = KudosWebSocketRegistry()
        val s1 = StubSessionRef("s1", userId = "u1", tenantId = "t1")
        val s2 = StubSessionRef("s2", userId = "u1", tenantId = "t1")
        val s3 = StubSessionRef("s3", userId = "u1", tenantId = "t2")

        r.register(s1)
        r.register(s2)
        r.register(s3)

        val byUser = r.findByUserId("u1").map { it.sessionId }.toSet()
        assertEquals(setOf("s1", "s2", "s3"), byUser, "All sessions of the same user across devices should be listed")
        assertEquals(setOf("s1", "s2"), r.findByTenantId("t1").map { it.sessionId }.toSet())
        assertEquals(setOf("s3"), r.findByTenantId("t2").map { it.sessionId }.toSet())
    }

    @Test
    fun unregister_removesFromAllIndexes() {
        val r = KudosWebSocketRegistry()
        val s = StubSessionRef("s1", userId = "u1", tenantId = "t1")
        r.register(s)

        r.unregister("s1")

        assertEquals(0, r.size)
        assertNull(r.findById("s1"))
        assertEquals(emptyList(), r.findByUserId("u1"))
        assertEquals(emptyList(), r.findByTenantId("t1"))
    }

    @Test
    fun unregister_unknownSessionId_isNoOp() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1", userId = "u1"))

        r.unregister("nope")

        assertEquals(1, r.size, "An unknown sessionId should not affect existing sessions")
    }

    @Test
    fun unregister_oneOfMultiSessions_keepsOthersInUserIndex() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1", userId = "u1"))
        r.register(StubSessionRef("s2", userId = "u1"))

        r.unregister("s1")

        val byUser = r.findByUserId("u1").map { it.sessionId }.toSet()
        assertEquals(setOf("s2"), byUser, "Other sessions of u1 should be retained")
    }

    @Test
    fun unregister_lastSessionInUserBucket_dropsBucket() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1", userId = "u1"))

        r.unregister("s1")

        // After the last one is removed, the user / tenant bucket should be dropped entirely.
        assertEquals(emptyList(), r.findByUserId("u1"))
    }

    @Test
    fun all_returnsSnapshot_notLiveView() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1"))
        r.register(StubSessionRef("s2"))
        val snapshot = r.all()

        r.unregister("s1")

        assertEquals(2, snapshot.size, "all() should return a snapshot that does not change with subsequent unregisters")
        assertEquals(1, r.size)
    }

    @Test
    fun register_sameSessionIdTwice_replacesAndClearsTheReplacedSessionsIndexes() {
        val r = KudosWebSocketRegistry()
        val first = StubSessionRef("s1", userId = "u1", tenantId = "t1")
        val second = StubSessionRef("s1", userId = "u2", tenantId = "t2")

        r.register(first)
        r.register(second)

        assertSame(second, r.findById("s1"))
        // The replaced session's buckets must not keep resolving s1: findByUserId("u1") would
        // otherwise hand u1 a connection that now belongs to u2.
        assertEquals(emptyList(), r.findByUserId("u1"), "The replaced session's user bucket must be cleared")
        assertEquals(emptyList(), r.findByTenantId("t1"), "The replaced session's tenant bucket must be cleared")
        assertEquals(listOf("s1"), r.findByUserId("u2").map { it.sessionId })
        assertEquals(listOf("s1"), r.findByTenantId("t2").map { it.sessionId })
    }

    @Test
    fun register_sameSessionIdAndSameUser_keepsTheSessionIndexed() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1", userId = "u1"))
        val second = StubSessionRef("s1", userId = "u1")

        r.register(second)

        // The cleanup of the replaced entry must not remove the bucket entry the new session needs.
        assertEquals(listOf(second), r.findByUserId("u1"))
    }

    @Test
    fun unregisterBySession_doesNotEvictASuccessorHoldingTheSameId() {
        val r = KudosWebSocketRegistry()
        val stale = StubSessionRef("s1", userId = "u1")
        val successor = StubSessionRef("s1", userId = "u1")
        r.register(stale)
        r.register(successor)

        // The stale connection's route finally-block fires late; it must not take the live one down.
        r.unregister(stale)

        assertSame(successor, r.findById("s1"), "Only the still-registered instance may be removed")
        assertEquals(listOf(successor), r.findByUserId("u1"))
    }

    @Test
    fun unregisterBySession_removesWhenStillRegistered() {
        val r = KudosWebSocketRegistry()
        val s = StubSessionRef("s1", userId = "u1", tenantId = "t1")
        r.register(s)

        r.unregister(s)

        assertEquals(0, r.size)
        assertEquals(emptyList(), r.findByUserId("u1"))
        assertEquals(emptyList(), r.findByTenantId("t1"))
    }

    /**
     * Regression test for the register/unregister race on a secondary index.
     *
     * Churn threads repeatedly register and unregister sessions of user "u", which makes the
     * `byUserId["u"]` bucket empty (and therefore removed) over and over. Meanwhile a registrar
     * thread adds long-lived sessions for the same user. If the "add to bucket" step ran outside the
     * map's per-key compute lock, a registrar could add its sessionId to a bucket that a churn
     * thread had just detached from the map — leaving a live, connected session permanently
     * invisible to [KudosWebSocketRegistry.findByUserId], i.e. silently unreachable by broadcasts.
     *
     * The assertion is exact (all survivors visible) whichever way the threads interleave, so a
     * correct implementation cannot fail this test intermittently.
     */
    @Test
    fun concurrentRegisterAndUnregister_neverLosesALiveSessionFromTheUserIndex() {
        val r = KudosWebSocketRegistry()
        val user = "u"
        val churnThreads = 6
        val churnIterations = 400
        val survivors = 150

        val pool = Executors.newFixedThreadPool(churnThreads + 1)
        val start = CountDownLatch(1)
        val done = CountDownLatch(churnThreads + 1)
        try {
            repeat(churnThreads) { t ->
                pool.execute {
                    start.await()
                    repeat(churnIterations) { i ->
                        val s = StubSessionRef("churn-$t-$i", userId = user)
                        r.register(s)
                        r.unregister(s)
                    }
                    done.countDown()
                }
            }
            pool.execute {
                start.await()
                repeat(survivors) { i ->
                    r.register(StubSessionRef("survivor-$i", userId = user))
                    Thread.yield()
                }
                done.countDown()
            }
            start.countDown()
            assertTrue(done.await(60, TimeUnit.SECONDS), "Concurrency workload did not finish in time")
        } finally {
            pool.shutdownNow()
        }

        val visible = r.findByUserId(user).map { it.sessionId }.toSet()
        assertEquals(
            (0 until survivors).map { "survivor-$it" }.toSet(),
            visible,
            "Every still-registered session must remain reachable through the user index",
        )
        assertEquals(survivors, r.size, "Only the survivors may remain in the primary index")
    }

    /** Plain-data ref with no Ktor dependency, used for testing. */
    private class StubSessionRef(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {}
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }
}
