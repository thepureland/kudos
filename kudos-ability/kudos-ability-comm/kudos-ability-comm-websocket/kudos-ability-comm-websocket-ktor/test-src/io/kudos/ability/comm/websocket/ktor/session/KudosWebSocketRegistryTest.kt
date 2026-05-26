package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.websocket.CloseReason
import java.util.concurrent.ConcurrentHashMap
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

        // After the last one is removed, the user / tenant bucket should be dropped entirely (the "if isNullOrEmpty -> null" rule stated in the README).
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
    fun register_sameSessionIdTwice_replaces() {
        val r = KudosWebSocketRegistry()
        val first = StubSessionRef("s1", userId = "u1")
        val second = StubSessionRef("s1", userId = "u2")

        r.register(first)
        r.register(second)

        // Primary index is overwritten
        assertSame(second, r.findById("s1"))
        // Secondary indexes: the original u1 bucket is not cleared — the README states "sessionId uniqueness is not enforced, the business side is responsible for it"
        assertTrue(r.findByUserId("u1").any { it.sessionId == "s1" })
        assertTrue(r.findByUserId("u2").any { it.sessionId == "s1" })
    }

    /** Plain-data ref with no Ktor dependency, used for testing. */
    private class StubSessionRef(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {}
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: CloseReason) {}
    }
}
