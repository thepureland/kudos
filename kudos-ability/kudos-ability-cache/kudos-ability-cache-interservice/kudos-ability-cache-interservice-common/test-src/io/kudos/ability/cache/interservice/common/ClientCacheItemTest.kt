package io.kudos.ability.cache.interservice.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for [ClientCacheItem.genUid] fingerprint stability and edge cases.
 *
 * Contract (README):
 *  - Same class, same field values → same UID
 *  - Different field values → different UID
 *  - FQN and JSON are separated by `#`, preventing "class name + JSON concatenated to the same string" collisions
 *  - Not for encryption — MD5 is used only as a content fingerprint
 *
 * Known risk (also documented in the README): when a DTO contains a `Map<*, *>` that is not a LinkedHashMap,
 * iteration order is unstable — these tests deliberately use [LinkedHashMap] / data class for repeatability.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheItemTest {

    /**
     * User DTO used for UID tests.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private data class UserDto(val id: Int, val name: String)

    /**
     * Order DTO used to test UID type isolation.
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
        // UserDto(1, "Alice") and OrderDto(1, "Alice") produce identical JSON; only the FQN differs.
        // Without the `#` separator, class name + JSON concatenation would collide — this test guards FQN isolation.
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
