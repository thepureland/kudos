package io.kudos.ms.sys.common.locale.vo

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
 * test for SysLocaleCacheEntry
 *
 * Covers property accessors, IIdEntity contract, nullable remark, data-class contract
 * and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleCacheEntryTest {

    private fun entry() = SysLocaleCacheEntry(
        id = "l-1",
        code = "zh_CN",
        displayName = "简体中文",
        englishName = "Simplified Chinese",
        sortNo = 5,
        remark = "r",
        active = true,
        builtIn = false,
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("l-1", e.id)
        assertEquals("zh_CN", e.code)
        assertEquals("简体中文", e.displayName)
        assertEquals("Simplified Chinese", e.englishName)
        assertEquals(5, e.sortNo)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableRemark() {
        assertNull(entry().copy(remark = null).remark)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "en_US"))
        assertTrue(a.toString().contains("zh_CN"))
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
