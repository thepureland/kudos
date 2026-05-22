package io.kudos.ability.cache.interservice.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * [ClientCacheItem.genUid] 指纹稳定性 + 边界场景单测。
 *
 * 契约（README）：
 *  - 同一类、同一字段值 → 同一 UID
 *  - 不同字段值 → 不同 UID
 *  - FQN 与 JSON 间用 `#` 分隔，杜绝"类名 + JSON 拼接同字符串"碰撞
 *  - 与"加密"无关——MD5 仅作内容指纹
 *
 * 已知风险（README 也说过）：DTO 含 `Map<*, *>` 且非 LinkedHashMap 时迭代顺序不稳——
 * 在本测试中刻意用 [LinkedHashMap] / data class 保证可重复。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheItemTest {

    /**
     * UID 测试用用户 DTO。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private data class UserDto(val id: Int, val name: String)

    /**
     * UID 类型隔离测试用订单 DTO。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private data class OrderDto(val id: Int, val name: String)

    @Test
    fun genUid_sameClassSameFields_sameUid() {
        val a = UserDto(1, "Alice")
        val b = UserDto(1, "Alice")
        assertEquals(ClientCacheItem.genUid(a), ClientCacheItem.genUid(b))
    }

    @Test
    fun genUid_sameClassDifferentFields_differentUid() {
        val a = UserDto(1, "Alice")
        val b = UserDto(2, "Alice")
        assertNotEquals(ClientCacheItem.genUid(a), ClientCacheItem.genUid(b))
    }

    @Test
    fun genUid_differentClassesSameFieldShape_differentUid() {
        // UserDto(1, "Alice") 与 OrderDto(1, "Alice") JSON 内容完全一致，仅 FQN 不同。
        // 没有 # 分隔时类名 + JSON 拼接会撞——本测试守住 FQN 隔离。
        val u = UserDto(1, "Alice")
        val o = OrderDto(1, "Alice")
        assertNotEquals(ClientCacheItem.genUid(u), ClientCacheItem.genUid(o))
    }

    @Test
    fun genUid_returnsNonBlankString() {
        val uid = ClientCacheItem.genUid(UserDto(1, "x"))
        assertNotNull(uid)
        assert(uid.isNotBlank())
    }

    @Test
    fun construct_uidAndDataExposed() {
        val item = ClientCacheItem("uid-1", "payload")
        assertEquals("uid-1", item.uuid)
        assertEquals("payload", item.cacheData)
    }

    @Test
    fun defaultConstructor_fieldsNull() {
        val item = ClientCacheItem()
        assertEquals(null, item.uuid)
        assertEquals(null, item.cacheData)
    }

    @Test
    fun toSnapshot_recordsTypeAndJsonPayload() {
        val item = ClientCacheItem("uid-1", UserDto(7, "Bob"))

        val snapshot = item.toSnapshot()

        assertEquals("uid-1", snapshot.uuid)
        assertEquals(UserDto::class.java.name, snapshot.cacheDataType)
        assertEquals("""{"id":7,"name":"Bob"}""", snapshot.cacheDataJson)
    }

    @Test
    fun jsonSnapshot_roundTripsThroughCallerDecoder() {
        val item = ClientCacheItem("uid-1", UserDto(7, "Bob"))
        val json = item.toJsonSnapshot()

        val restored = ClientCacheItem.fromJsonSnapshot(json) { cacheDataType, cacheDataJson ->
            assertEquals(UserDto::class.java.name, cacheDataType)
            cacheDataJson
        }

        assertEquals("uid-1", restored.uuid)
        assertEquals("""{"id":7,"name":"Bob"}""", restored.cacheData)
    }

    @Test
    fun snapshot_nullPayload_staysNull() {
        val snapshot = ClientCacheItem().apply { uuid = "uid-empty" }.toSnapshot()

        val restored = ClientCacheItem.fromSnapshot(snapshot) { cacheDataType, cacheDataJson ->
            assertEquals(null, cacheDataType)
            assertEquals(null, cacheDataJson)
            null
        }

        assertEquals("uid-empty", restored.uuid)
        assertEquals(null, restored.cacheData)
    }
}
