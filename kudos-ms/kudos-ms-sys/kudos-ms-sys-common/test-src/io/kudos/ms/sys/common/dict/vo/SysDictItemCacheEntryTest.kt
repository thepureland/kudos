package io.kudos.ms.sys.common.dict.vo

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
 * test for SysDictItemCacheEntry
 *
 * Covers property accessors (including nullable orderNum/parentId/remark),
 * data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemCacheEntryTest {

    private fun entry() = SysDictItemCacheEntry(
        id = "i-1",
        itemCode = "male",
        itemName = "Male",
        dictId = "d-1",
        orderNum = 1,
        parentId = null,
        remark = "remark",
        active = true,
        builtIn = true,
        dictType = "gender",
        dictName = "Gender",
        atomicServiceCode = "sys",
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("i-1", e.id)
        assertEquals("male", e.itemCode)
        assertEquals("Male", e.itemName)
        assertEquals("d-1", e.dictId)
        assertEquals(1, e.orderNum)
        assertNull(e.parentId)
        assertEquals("remark", e.remark)
        assertTrue(e.active)
        assertTrue(e.builtIn)
        assertEquals("gender", e.dictType)
        assertEquals("Gender", e.dictName)
        assertEquals("sys", e.atomicServiceCode)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(orderNum = null))
        assertNotEquals(a, a.copy(parentId = "p-1"))
        assertTrue(a.toString().contains("male"))
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
