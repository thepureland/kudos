package io.kudos.ability.cache.common.notify

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the [CacheMessage] value object.
 *
 * Coverage:
 *  - No-arg constructor leaves every field null.
 *  - Two-arg constructor sets cacheName / key and leaves nodeId / cacheType null.
 *  - All mutable properties are settable (including null key and Unicode cacheName).
 *  - Java serialization round-trips the payload (the class is transmitted across processes via MQ).
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheMessageTest {

    @Test
    fun noArgConstructor_allFieldsNull() {
        val m = CacheMessage()
        assertNull(m.cacheName)
        assertNull(m.key)
        assertNull(m.nodeId)
        assertNull(m.cacheType)
    }

    @Test
    fun twoArgConstructor_setsNameAndKey_othersNull() {
        val m = CacheMessage("USER", 42)
        assertEquals("USER", m.cacheName)
        assertEquals(42, m.key)
        assertNull(m.nodeId)
        assertNull(m.cacheType)
    }

    @Test
    fun twoArgConstructor_acceptsNulls() {
        val m = CacheMessage(null, null)
        assertNull(m.cacheName)
        assertNull(m.key)
    }

    @Test
    fun mutableProperties_areSettable() {
        val m = CacheMessage().apply {
            cacheName = "缓存名"   // Unicode name
            key = "用户:1"
            nodeId = "node-1"
            cacheType = "hash"
        }
        assertEquals("缓存名", m.cacheName)
        assertEquals("用户:1", m.key)
        assertEquals("node-1", m.nodeId)
        assertEquals("hash", m.cacheType)
    }

    @Test
    fun javaSerialization_roundTripsPayload() {
        val original = CacheMessage("ORDER", "k1").apply {
            nodeId = "n7"
            cacheType = "kv"
        }
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as CacheMessage }

        assertEquals("ORDER", restored.cacheName)
        assertEquals("k1", restored.key)
        assertEquals("n7", restored.nodeId)
        assertEquals("kv", restored.cacheType)
    }
}
