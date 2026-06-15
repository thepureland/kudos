package io.kudos.ms.sys.common.dict.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictCacheEntry
 *
 * Covers property accessors, data-class contract (equals/hashCode/toString/copy)
 * and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictCacheEntryTest {

    private fun entry() = SysDictCacheEntry(
        id = "d-1",
        dictType = "gender",
        dictName = "Gender",
        atomicServiceCode = "sys",
        remark = null,
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("d-1", e.id)
        assertEquals("gender", e.dictType)
        assertEquals("Gender", e.dictName)
        assertEquals("sys", e.atomicServiceCode)
        assertNull(e.remark)
        assertTrue(e.active)
        assertFalse(e.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertNotEquals(a, a.copy(remark = "r"))
        assertTrue(a.toString().contains("gender"))
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
