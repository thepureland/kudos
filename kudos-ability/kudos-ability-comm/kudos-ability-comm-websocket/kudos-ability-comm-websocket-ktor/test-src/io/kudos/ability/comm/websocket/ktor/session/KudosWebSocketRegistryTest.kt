package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.websocket.CloseReason
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [KudosWebSocketRegistry] 三套索引（id / userId / tenantId）的注册 / 注销单测。
 *
 * 使用 [StubSessionRef] 而非真正的 [KudosWebSocketSession]——registry 只读取元数据字段，
 * 用 stub 避开 Ktor 依赖。
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
        // 匿名 session 不应进 user / tenant 索引——同名查不到任何东西
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
        assertEquals(setOf("s1", "s2", "s3"), byUser, "同一用户多端在线全部列出")
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

        assertEquals(1, r.size, "未知 sessionId 不应影响已有会话")
    }

    @Test
    fun unregister_oneOfMultiSessions_keepsOthersInUserIndex() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1", userId = "u1"))
        r.register(StubSessionRef("s2", userId = "u1"))

        r.unregister("s1")

        val byUser = r.findByUserId("u1").map { it.sessionId }.toSet()
        assertEquals(setOf("s2"), byUser, "应保留 u1 名下的其他会话")
    }

    @Test
    fun unregister_lastSessionInUserBucket_dropsBucket() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1", userId = "u1"))

        r.unregister("s1")

        // 最后一个被剔后，user / tenant 桶应该被整体丢掉（README 段说明的"if isNullOrEmpty -> null"）
        assertEquals(emptyList(), r.findByUserId("u1"))
    }

    @Test
    fun all_returnsSnapshot_notLiveView() {
        val r = KudosWebSocketRegistry()
        r.register(StubSessionRef("s1"))
        r.register(StubSessionRef("s2"))
        val snapshot = r.all()

        r.unregister("s1")

        assertEquals(2, snapshot.size, "all() 应返回快照，不随后续注销变化")
        assertEquals(1, r.size)
    }

    @Test
    fun register_sameSessionIdTwice_replaces() {
        val r = KudosWebSocketRegistry()
        val first = StubSessionRef("s1", userId = "u1")
        val second = StubSessionRef("s1", userId = "u2")

        r.register(first)
        r.register(second)

        // 主索引覆盖
        assertSame(second, r.findById("s1"))
        // 二级索引：原 u1 桶不会被清——README 已声明"不强制 sessionId 唯一，业务侧自保"
        assertTrue(r.findByUserId("u1").any { it.sessionId == "s1" })
        assertTrue(r.findByUserId("u2").any { it.sessionId == "s1" })
    }

    /** 无 Ktor 依赖的纯数据 ref，用于测试。 */
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
