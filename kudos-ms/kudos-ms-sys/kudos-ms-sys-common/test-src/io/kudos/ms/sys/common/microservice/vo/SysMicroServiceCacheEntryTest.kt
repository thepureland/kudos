package io.kudos.ms.sys.common.microservice.vo

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
 * test for SysMicroServiceCacheEntry
 *
 * Covers property accessors (including nullable parentCode / remark), IIdEntity contract,
 * data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceCacheEntryTest {

    private fun entry() = SysMicroServiceCacheEntry(
        id = "ms_a",
        code = "ms_a",
        name = "Service A",
        context = "/svc-a",
        atomicService = true,
        parentCode = "parent",
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("ms_a", e.id)
        assertEquals("/svc-a", e.context)
        assertTrue(e.atomicService)
        assertEquals("parent", e.parentCode)
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
        assertNotEquals(a, a.copy(code = "ms_b"))
        assertTrue(a.toString().contains("ms_a"))
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
