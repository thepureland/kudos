package io.kudos.ms.sys.common.param.vo

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
 * test for SysParamCacheEntry
 *
 * Covers property accessors, IIdEntity contract, nullable fields, data-class contract
 * and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamCacheEntryTest {

    private fun entry() = SysParamCacheEntry(
        id = "p-1",
        paramName = "max.size",
        paramValue = "100",
        defaultValue = "50",
        atomicServiceCode = "sys",
        orderNum = 1,
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("p-1", e.id)
        assertEquals("max.size", e.paramName)
        assertEquals("100", e.paramValue)
        assertEquals("50", e.defaultValue)
        assertEquals("sys", e.atomicServiceCode)
        assertEquals(1, e.orderNum)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(defaultValue = null, orderNum = null, remark = null)
        assertNull(e.defaultValue)
        assertNull(e.orderNum)
        assertNull(e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(paramValue = "200"))
        assertTrue(a.toString().contains("max.size"))
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
