package io.kudos.ms.sys.common.accessrule.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleCacheEntry
 *
 * Covers property accessors, data-class contract (equals/hashCode/toString/copy)
 * and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleCacheEntryTest {

    private fun entry() = SysAccessRuleCacheEntry(
        id = "id-1",
        tenantId = "",
        systemCode = "sys-a",
        accessRuleTypeDictCode = "2",
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("id-1", e.id)
        assertEquals("", e.tenantId)
        assertEquals("sys-a", e.systemCode)
        assertEquals("2", e.accessRuleTypeDictCode)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "id-2"))
        assertNotEquals(a, a.copy(tenantId = "t-1"))
        assertTrue(a.toString().contains("sys-a"))
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
