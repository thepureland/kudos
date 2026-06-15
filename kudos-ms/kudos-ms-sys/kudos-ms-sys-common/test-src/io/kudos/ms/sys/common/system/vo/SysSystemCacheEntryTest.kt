package io.kudos.ms.sys.common.system.vo

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
 * test for SysSystemCacheEntry
 *
 * Covers property accessors (including nullable parentCode / remark), IIdEntity contract,
 * data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemCacheEntryTest {

    private fun entry() = SysSystemCacheEntry(
        id = "sys",
        code = "sys",
        name = "System",
        subSystem = false,
        parentCode = "root",
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("sys", e.id)
        assertEquals("sys", e.code)
        assertEquals("System", e.name)
        assertEquals(false, e.subSystem)
        assertEquals("root", e.parentCode)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(parentCode = null, remark = null)
        assertNull(e.parentCode)
        assertNull(e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("sys"))
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
