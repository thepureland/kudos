package io.kudos.ms.sys.common.tenant.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for SysTenantSystemCacheEntry
 *
 * Covers property accessors, IIdEntity contract, data-class contract and Java
 * serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantSystemCacheEntryTest {

    private fun entry() = SysTenantSystemCacheEntry(
        id = "ts-1",
        tenantId = "t-1",
        systemCode = "sys",
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("ts-1", e.id)
        assertEquals("t-1", e.tenantId)
        assertEquals("sys", e.systemCode)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(systemCode = "other"))
        assertTrue(a.toString().contains("ts-1"))
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
