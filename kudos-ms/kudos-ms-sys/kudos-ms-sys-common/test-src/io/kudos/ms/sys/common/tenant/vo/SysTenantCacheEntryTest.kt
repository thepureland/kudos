package io.kudos.ms.sys.common.tenant.vo

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
 * test for SysTenantCacheEntry
 *
 * Covers property accessors (including nullable timezone / defaultLanguageCode / remark),
 * IIdEntity contract, data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantCacheEntryTest {

    private fun entry() = SysTenantCacheEntry(
        id = "t-1",
        name = "Tenant One",
        timezone = "Asia/Shanghai",
        defaultLanguageCode = "zh-CN",
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("t-1", e.id)
        assertEquals("Tenant One", e.name)
        assertEquals("Asia/Shanghai", e.timezone)
        assertEquals("zh-CN", e.defaultLanguageCode)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(timezone = null, defaultLanguageCode = null, remark = null)
        assertNull(e.timezone)
        assertNull(e.defaultLanguageCode)
        assertNull(e.remark)
    }

    @Test
    fun unicodeName() {
        val e = entry().copy(name = "租户·特例🌟")
        assertEquals("租户·特例🌟", e.name)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("t-1"))
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
