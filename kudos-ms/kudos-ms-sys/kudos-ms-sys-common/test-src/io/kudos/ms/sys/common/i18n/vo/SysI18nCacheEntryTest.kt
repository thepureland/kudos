package io.kudos.ms.sys.common.i18n.vo

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
 * test for SysI18nCacheEntry
 *
 * Covers property accessors (including nullable id), data-class contract
 * (equals/hashCode/toString/copy) and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nCacheEntryTest {

    private fun entry() = SysI18nCacheEntry(
        id = "i-1",
        locale = "zh_CN",
        atomicServiceCode = "sys",
        i18nTypeDictCode = "ui",
        namespace = "common",
        key = "ok",
        value = "确定",
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("i-1", e.id)
        assertEquals("zh_CN", e.locale)
        assertEquals("sys", e.atomicServiceCode)
        assertEquals("ui", e.i18nTypeDictCode)
        assertEquals("common", e.namespace)
        assertEquals("ok", e.key)
        assertEquals("确定", e.value)
    }

    @Test
    fun nullableId() {
        val e = entry().copy(id = null)
        assertNull(e.id)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(value = "OK"))
        assertNotEquals(a, a.copy(id = null))
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
