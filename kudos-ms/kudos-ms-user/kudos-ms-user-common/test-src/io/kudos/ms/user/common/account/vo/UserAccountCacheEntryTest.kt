package io.kudos.ms.user.common.account.vo

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
 * test for UserAccountCacheEntry
 *
 * Covers property accessors, optional freeze-* defaults (null), data-class contract and
 * Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountCacheEntryTest {

    private fun entry() = UserAccountCacheEntry(
        id = "u-1",
        username = "alice",
        tenantId = "t-1",
        loginPassword = "lp",
        securityPassword = "sp",
        accountTypeDictCode = "1",
        accountStatusDictCode = "0",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        lastLoginTime = LocalDateTime.of(2026, 6, 15, 9, 0),
        lastLoginIp = 16909060L,
        lastLogoutTime = null,
        loginErrorTimes = 0,
        securityPasswordErrorTimes = 0,
        sessionKey = "sk",
        authenticationKey = "ak",
        orgId = "o-1",
        supervisorId = "s-1",
        remark = "r",
        active = true,
        builtIn = false,
        createUserId = "c",
        createUserName = "creator",
        createTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        updateUserId = "u",
        updateUserName = "updater",
        updateTime = LocalDateTime.of(2026, 2, 1, 0, 0),
    )

    @Test
    fun propertiesAndFreezeDefaults() {
        val e = entry()
        assertEquals("u-1", e.id)
        assertEquals("alice", e.username)
        assertEquals(16909060L, e.lastLoginIp)
        assertEquals(true, e.active)
        // freeze-* default to null when omitted
        assertNull(e.freezeType)
        assertNull(e.freezeTime)
        assertNull(e.freezeStartTime)
        assertNull(e.freezeEndTime)
        assertNull(e.freezeTitle)
        assertNull(e.freezeContent)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "u-2"))
        assertNotEquals(a, a.copy(freezeType = "1"))
        assertTrue(a.toString().contains("alice"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = entry().copy(freezeType = "1", freezeTitle = "frozen")
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
