package io.kudos.ms.user.common.account.support

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure-logic tests for the exit-boundary credential erasure (eraseCredentials):
 * the credential fields must come back null while every profile field is preserved,
 * and the original object must stay untouched (defensive copy).
 *
 * No DB / Spring needed: the functions are pure copy helpers on data classes.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountCredentialsErasureTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 0)

    private fun fullCacheEntry() = UserAccountCacheEntry(
        id = "u-1",
        username = "alice",
        tenantId = "t-1",
        loginPassword = "bcrypt-login-hash",
        securityPassword = "bcrypt-security-hash",
        accountTypeDictCode = "normal",
        accountStatusDictCode = "active",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        lastLoginTime = now,
        lastLoginIp = 0x7F000001L,
        lastLogoutTime = now,
        loginErrorTimes = 2,
        securityPasswordErrorTimes = 1,
        sessionKey = "session-key",
        authenticationKey = "totp-secret",
        orgId = "org-1",
        supervisorId = "u-0",
        remark = "remark",
        freezeType = "manual",
        freezeTime = now,
        freezeStartTime = now,
        freezeEndTime = now.plusHours(1),
        freezeTitle = "title",
        freezeContent = "content",
        active = true,
        builtIn = false,
        createUserId = "c-1",
        createUserName = "creator",
        createTime = now,
        updateUserId = "m-1",
        updateUserName = "modifier",
        updateTime = now,
    )

    @Test
    fun cacheEntry_credentialFieldsAreErased() {
        val sanitized = fullCacheEntry().eraseCredentials()
        assertNull(sanitized.loginPassword)
        assertNull(sanitized.securityPassword)
        assertNull(sanitized.authenticationKey)
        assertNull(sanitized.sessionKey)
    }

    @Test
    fun cacheEntry_profileFieldsArePreserved() {
        val original = fullCacheEntry()
        val sanitized = original.eraseCredentials()
        // Erasing the 4 credential fields of the original must yield exactly the sanitized copy:
        // proves no other field was altered, without asserting each of the 30+ fields one by one.
        assertEquals(
            original.copy(
                loginPassword = null,
                securityPassword = null,
                authenticationKey = null,
                sessionKey = null,
            ),
            sanitized
        )
        assertEquals("alice", sanitized.username)
        assertEquals(2, sanitized.loginErrorTimes)
        assertEquals("manual", sanitized.freezeType)
    }

    @Test
    fun cacheEntry_originalIsNotMutated() {
        val original = fullCacheEntry()
        original.eraseCredentials()
        assertEquals("bcrypt-login-hash", original.loginPassword)
        assertEquals("totp-secret", original.authenticationKey)
        assertEquals("session-key", original.sessionKey)
    }

    @Test
    fun detail_credentialFieldsAreErasedAndProfilePreserved() {
        val detail = UserAccountDetail(
            id = "u-1",
            username = "alice",
            loginPassword = "hash",
            securityPassword = "hash2",
            sessionKey = "sk",
            authenticationKey = "ak",
            orgId = "org-1",
        )
        val sanitized = detail.eraseCredentials()
        assertNull(sanitized.loginPassword)
        assertNull(sanitized.securityPassword)
        assertNull(sanitized.sessionKey)
        assertNull(sanitized.authenticationKey)
        assertEquals("u-1", sanitized.id)
        assertEquals("alice", sanitized.username)
        assertEquals("org-1", sanitized.orgId)
    }

    @Test
    fun edit_credentialFieldsAreErasedAndProfilePreserved() {
        val edit = UserAccountEdit(
            id = "u-1",
            username = "alice",
            loginPassword = "hash",
            securityPassword = "hash2",
            sessionKey = "sk",
            authenticationKey = "ak",
            remark = "r",
        )
        val sanitized = edit.eraseCredentials()
        assertNull(sanitized.loginPassword)
        assertNull(sanitized.securityPassword)
        assertNull(sanitized.sessionKey)
        assertNull(sanitized.authenticationKey)
        assertEquals("alice", sanitized.username)
        assertEquals("r", sanitized.remark)
    }

    @Test
    fun row_credentialFieldsAreErasedAndProfilePreserved() {
        // The row VO has no password hash fields; only sessionKey + authenticationKey are sensitive.
        val row = UserAccountRow(
            id = "u-1",
            username = "alice",
            sessionKey = "sk",
            authenticationKey = "ak",
            loginErrorTimes = 3,
        )
        val sanitized = row.eraseCredentials()
        assertNull(sanitized.sessionKey)
        assertNull(sanitized.authenticationKey)
        assertEquals("alice", sanitized.username)
        assertEquals(3, sanitized.loginErrorTimes)
    }
}
