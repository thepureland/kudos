package io.kudos.ms.user.common.contact.vo

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
 * test for UserContactWayCacheEntry
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayCacheEntryTest {

    private fun entry() = UserContactWayCacheEntry(
        id = "cw-1",
        userId = "u-1",
        contactWayDictCode = "email",
        contactWayValue = "a@b.com",
        contactWayStatusDictCode = "1",
        priority = 10,
        remark = "primary",
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("cw-1", e.id)
        assertEquals("email", e.contactWayDictCode)
        assertEquals("a@b.com", e.contactWayValue)
        assertEquals(10, e.priority)
        assertEquals("primary", e.remark)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(priority = null, remark = null)
        assertNull(e.priority)
        assertNull(e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(priority = 99))
        assertTrue(a.toString().contains("email"))
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
