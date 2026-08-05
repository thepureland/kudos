package io.kudos.ms.user.common.org.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for UserOrgCacheEntry
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgCacheEntryTest {

    private fun entry() = UserOrgCacheEntry(
        id = "o-1",
        name = "HQ",
        shortName = "hq",
        tenantId = "t-1",
        parentId = null,
        orgTypeDictCode = "1",
        sortNum = 1,
        remark = "root",
        active = true,
        builtIn = true,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("o-1", e.id)
        assertEquals("HQ", e.name)
        assertEquals("hq", e.shortName)
        assertEquals(1, e.sortNum)
        assertEquals(true, e.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(name = "Branch"))
        assertTrue(a.toString().contains("HQ"))
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
