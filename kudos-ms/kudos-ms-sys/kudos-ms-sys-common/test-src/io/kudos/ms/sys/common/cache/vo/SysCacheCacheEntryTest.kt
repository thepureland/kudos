package io.kudos.ms.sys.common.cache.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheCacheEntry
 *
 * Covers property accessors, IIdEntity contract, nullable fields, data-class contract
 * and Java serialization round-trip (with non-null LocalDateTime fields).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheCacheEntryTest {

    private fun entry() = SysCacheCacheEntry(
        id = "c-1",
        name = "userCache",
        atomicServiceCode = "sys",
        strategyDictCode = "LOCAL",
        writeOnBoot = true,
        writeInTime = false,
        ttl = 60,
        remark = "r",
        active = true,
        builtIn = false,
        hash = true,
        createUserId = "u1",
        createUserName = "creator",
        createTime = LocalDateTime.of(2026, 6, 15, 10, 0),
        updateUserId = "u2",
        updateUserName = "updater",
        updateTime = LocalDateTime.of(2026, 6, 15, 11, 0),
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("c-1", e.id)
        assertEquals("userCache", e.name)
        assertEquals("LOCAL", e.strategyDictCode)
        assertEquals(true, e.writeOnBoot)
        assertEquals(false, e.writeInTime)
        assertEquals(60, e.ttl)
        assertEquals(true, e.hash)
        assertEquals("creator", e.createUserName)
        assertEquals(LocalDateTime.of(2026, 6, 15, 11, 0), e.updateTime)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(ttl = null, remark = null, createUserId = null, createTime = null, updateTime = null)
        assertNull(e.ttl)
        assertNull(e.remark)
        assertNull(e.createUserId)
        assertNull(e.createTime)
        assertNull(e.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertNotEquals(a, a.copy(hash = false))
        assertTrue(a.toString().contains("userCache"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = entry()
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
