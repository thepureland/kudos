package io.kudos.ms.user.common.account.vo

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
 * test for UserAccountThirdCacheEntry
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountThirdCacheEntryTest {

    private fun entry() = UserAccountThirdCacheEntry(
        id = "th-1",
        userId = "u-1",
        accountProviderDictCode = "wechat",
        accountProviderIssuer = "issuer",
        subject = "open-id-1",
        unionId = "union-1",
        externalDisplayName = "Alice",
        externalEmail = "a@b.com",
        avatarUrl = "http://x/y.png",
        lastLoginTime = LocalDateTime.of(2026, 6, 15, 8, 0),
        tenantId = "t-1",
        remark = "r",
        active = true,
        builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("th-1", e.id)
        assertEquals("wechat", e.accountProviderDictCode)
        assertEquals("open-id-1", e.subject)
        assertEquals("union-1", e.unionId)
        assertEquals("a@b.com", e.externalEmail)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(subject = "open-id-2"))
        assertTrue(a.toString().contains("wechat"))
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
