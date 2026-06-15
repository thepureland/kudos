package io.kudos.ms.user.common.login.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for UserLoginRememberMeCacheEntry
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLoginRememberMeCacheEntryTest {

    private fun entry() = UserLoginRememberMeCacheEntry(
        id = "rm-1",
        username = "alice",
        token = "tok-1",
        lastUsed = LocalDateTime.of(2026, 6, 15, 7, 30),
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("rm-1", e.id)
        assertEquals("alice", e.username)
        assertEquals("tok-1", e.token)
        assertEquals(LocalDateTime.of(2026, 6, 15, 7, 30), e.lastUsed)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(token = "tok-2"))
        assertTrue(a.toString().contains("alice"))
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
