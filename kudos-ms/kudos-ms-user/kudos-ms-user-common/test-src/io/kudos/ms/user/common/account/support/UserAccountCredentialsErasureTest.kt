package io.kudos.ms.user.common.account.support

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * test for UserAccountCredentialsErasure extension functions.
 *
 * Covers, for each of the four overloads:
 *  - all documented credential fields are nulled out
 *  - non-credential fields are preserved unchanged
 *  - the original instance is NOT mutated (defensive copy) and a new instance is returned
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountCredentialsErasureTest {

    @Test
    fun cacheEntryEraseCredentials() {
        val original = UserAccountCacheEntry(
            id = "u-1",
            username = "alice",
            tenantId = "t-1",
            loginPassword = "bcrypt-login",
            securityPassword = "bcrypt-sec",
            accountTypeDictCode = "1",
            accountStatusDictCode = "0",
            defaultLocale = "zh_CN",
            defaultTimezone = "Asia/Shanghai",
            defaultCurrency = "CNY",
            lastLoginTime = null,
            lastLoginIp = 123L,
            lastLogoutTime = null,
            loginErrorTimes = 2,
            securityPasswordErrorTimes = 1,
            sessionKey = "sess-secret",
            authenticationKey = "totp-secret",
            orgId = "o-1",
            supervisorId = "s-1",
            remark = "r",
            active = true,
            builtIn = false,
            createUserId = null,
            createUserName = null,
            createTime = null,
            updateUserId = null,
            updateUserName = null,
            updateTime = null,
        )

        val sanitized = original.eraseCredentials()

        // credentials nulled
        assertNull(sanitized.loginPassword)
        assertNull(sanitized.securityPassword)
        assertNull(sanitized.authenticationKey)
        assertNull(sanitized.sessionKey)
        // non-credentials preserved
        assertEquals("u-1", sanitized.id)
        assertEquals("alice", sanitized.username)
        assertEquals("t-1", sanitized.tenantId)
        assertEquals("1", sanitized.accountTypeDictCode)
        assertEquals(123L, sanitized.lastLoginIp)
        assertEquals(2, sanitized.loginErrorTimes)
        assertEquals("o-1", sanitized.orgId)
        assertEquals(true, sanitized.active)
        // original untouched + new instance
        assertNotSame(original, sanitized)
        assertEquals("bcrypt-login", original.loginPassword)
        assertEquals("totp-secret", original.authenticationKey)
        assertEquals("sess-secret", original.sessionKey)
        assertEquals("bcrypt-sec", original.securityPassword)
    }

    @Test
    fun detailEraseCredentials() {
        val original = UserAccountDetail(
            id = "u-1",
            username = "alice",
            tenantId = "t-1",
            loginPassword = "p",
            securityPassword = "s",
            sessionKey = "sk",
            authenticationKey = "ak",
            orgId = "o-1",
        )
        val sanitized = original.eraseCredentials()
        assertNull(sanitized.loginPassword)
        assertNull(sanitized.securityPassword)
        assertNull(sanitized.authenticationKey)
        assertNull(sanitized.sessionKey)
        assertEquals("alice", sanitized.username)
        assertEquals("o-1", sanitized.orgId)
        assertNotSame(original, sanitized)
        assertEquals("p", original.loginPassword)
        assertEquals("ak", original.authenticationKey)
    }

    @Test
    fun editEraseCredentials() {
        val original = UserAccountEdit(
            id = "u-1",
            username = "alice",
            loginPassword = "p",
            securityPassword = "s",
            sessionKey = "sk",
            authenticationKey = "ak",
            remark = "keep-me",
        )
        val sanitized = original.eraseCredentials()
        assertNull(sanitized.loginPassword)
        assertNull(sanitized.securityPassword)
        assertNull(sanitized.authenticationKey)
        assertNull(sanitized.sessionKey)
        assertEquals("alice", sanitized.username)
        assertEquals("keep-me", sanitized.remark)
        assertNotSame(original, sanitized)
        assertEquals("p", original.loginPassword)
        assertEquals("sk", original.sessionKey)
    }

    @Test
    fun rowEraseCredentials_onlyTwoFields() {
        // Row VO carries no password hashes; only authenticationKey + sessionKey are erased.
        val original = UserAccountRow(
            id = "u-1",
            username = "alice",
            sessionKey = "sk",
            authenticationKey = "ak",
            orgId = "o-1",
            active = true,
        )
        val sanitized = original.eraseCredentials()
        assertNull(sanitized.authenticationKey)
        assertNull(sanitized.sessionKey)
        assertEquals("alice", sanitized.username)
        assertEquals("o-1", sanitized.orgId)
        assertEquals(true, sanitized.active)
        assertNotSame(original, sanitized)
        assertEquals("ak", original.authenticationKey)
        assertEquals("sk", original.sessionKey)
    }

}
