package io.kudos.ms.sys.common.domain.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDomainCacheEntry
 *
 * Covers property accessors (including nullable tenantId = platform-level and remark),
 * IIdEntity contract, data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainCacheEntryTest {

    private fun entry() = SysDomainCacheEntry(
        id = "d-1",
        domain = "example.com",
        systemCode = "sys",
        tenantId = "t-1",
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("d-1", e.id)
        assertEquals("example.com", e.domain)
        assertEquals("sys", e.systemCode)
        assertEquals("t-1", e.tenantId)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(tenantId = null, remark = null)
        assertNull(e.tenantId)
        assertNull(e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(domain = "other.com"))
        assertTrue(a.toString().contains("d-1"))
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
