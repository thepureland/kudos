package io.kudos.ms.sys.common.datasource.vo

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
 * test for SysDataSourceCacheEntry
 *
 * Covers property accessors (including nullable microServiceCode), IIdEntity contract,
 * data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceCacheEntryTest {

    private fun entry() = SysDataSourceCacheEntry(
        id = "ds-1",
        name = "ds1",
        subSystemCode = "sys",
        microServiceCode = "ms",
        tenantId = "t-1",
        url = "jdbc:h2:mem:test",
        username = "sa",
        password = "pwd",
        initialSize = 1,
        maxActive = 10,
        maxIdle = 8,
        minIdle = 2,
        maxWait = 1000,
        maxAge = 60000,
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("ds-1", e.id)
        assertEquals("ms", e.microServiceCode)
        assertEquals("pwd", e.password)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(microServiceCode = null, tenantId = null, password = null, active = null, builtIn = null)
        assertNull(e.microServiceCode)
        assertNull(e.tenantId)
        assertNull(e.password)
        assertNull(e.active)
        assertNull(e.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "ds2"))
        assertTrue(a.toString().contains("ds-1"))
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
