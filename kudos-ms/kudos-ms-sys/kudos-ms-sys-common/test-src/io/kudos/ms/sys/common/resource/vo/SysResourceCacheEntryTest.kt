package io.kudos.ms.sys.common.resource.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceCacheEntry
 *
 * Covers property accessors (including all nullable fields), IIdEntity contract,
 * data-class contract and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceCacheEntryTest {

    private fun entry() = SysResourceCacheEntry(
        id = "r-1",
        name = "Menu",
        url = "/u",
        resourceTypeDictCode = "1",
        parentId = "p-1",
        orderNum = 2,
        icon = "ic",
        subSystemCode = "sys",
        remark = "r",
        active = true,
        builtIn = false,
        createUserId = "u1",
        createUserName = "User1",
        createTime = LocalDateTime.of(2026, 6, 15, 10, 0),
        updateUserId = "u2",
        updateUserName = "User2",
        updateTime = LocalDateTime.of(2026, 6, 15, 11, 0),
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("r-1", e.id)
        assertEquals("Menu", e.name)
        assertEquals("1", e.resourceTypeDictCode)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(
            name = null, url = null, resourceTypeDictCode = null, parentId = null,
            orderNum = null, icon = null, subSystemCode = null, remark = null,
            active = null, builtIn = null, createUserId = null, createUserName = null,
            createTime = null, updateUserId = null, updateUserName = null, updateTime = null,
        )
        assertNull(e.name)
        assertNull(e.active)
        assertNull(e.builtIn)
        assertNull(e.createTime)
        assertNull(e.updateTime)
        assertEquals("r-1", e.id)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("r-1"))
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
