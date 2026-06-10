package io.kudos.ms.user.core.passport.service

import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.service.impl.PassportService
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.apache.commons.codec.binary.Base32
import org.junit.jupiter.api.BeforeEach
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for PassportService
 *
 * Test data source: `PassportServiceTest.sql`
 *
 * Seeds one active=true user and one active=false user in the user table. `@BeforeEach`
 * BCrypt-hashes the password in place (cost=4 to speed up tests), then exercises the full
 * pipeline via [IPassportService.login].
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class PassportServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var passportService: IPassportService

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var userAccountService: IUserAccountService

    private val tenantId = "svc-tenant-passport-test"
    private val activeUserId = "b970f8c0-0000-0000-0000-000000000001"
    private val inactiveUserId = "b970f8c0-0000-0000-0000-000000000002"
    private val activeUsername = "svc-passport-test-active"
    private val inactiveUsername = "svc-passport-test-inactive"
    private val plainPassword = "test-password-123"

    @BeforeEach
    fun hashSeededPasswords() {
        // Compute the BCrypt hash once (cost=4 for testing, ~5ms) and update both seed users
        val hash = PasswordKit.hash(plainPassword, strength = 4)
        userAccountDao.updateProperties(activeUserId, mapOf(UserAccount::loginPassword.name to hash))
        userAccountDao.updateProperties(inactiveUserId, mapOf(UserAccount::loginPassword.name to hash))
        // Reset error counters to avoid leftovers from the previous round
        userAccountDao.updateProperties(activeUserId, mapOf(UserAccount::loginErrorTimes.name to 0))
        userAccountDao.updateProperties(inactiveUserId, mapOf(UserAccount::loginErrorTimes.name to 0))
        // Let the cache see the new password field
        userAccountHashCache.reloadAll(clear = true)
    }

    @Test
    fun login_success_returnsUserInfo() {
        val res = passportService.login(
            PassportLoginRequest(
                tenantId = tenantId,
                username = activeUsername,
                plainPassword = plainPassword,
            )
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        val info = assertNotNull(res.userInfo)
        assertEquals(activeUserId, info.id)
        assertEquals(activeUsername, info.username)
        assertEquals(tenantId, info.tenantId)
        assertNull(res.loginErrorTimes)
    }

    @Test
    fun login_wrongPassword_incrementsErrorCount() {
        val res = passportService.login(
            PassportLoginRequest(
                tenantId = tenantId,
                username = activeUsername,
                plainPassword = "wrong-password",
            )
        )
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        assertNull(res.userInfo)
        assertEquals(1, res.loginErrorTimes)
        // The DB should also be incremented by 1
        val after = assertNotNull(userAccountDao.get(activeUserId))
        assertEquals(1, after.loginErrorTimes)
    }

    @Test
    fun login_wrongPasswordTwice_accumulatesErrorCount() {
        passportService.login(
            PassportLoginRequest(tenantId, activeUsername, "wrong-1")
        )
        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, "wrong-2")
        )
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        assertEquals(2, res.loginErrorTimes)
    }

    @Test
    fun login_correctAfterWrong_resetsErrorCount() {
        // Fail once first
        passportService.login(PassportLoginRequest(tenantId, activeUsername, "wrong"))
        assertEquals(1, userAccountDao.get(activeUserId)?.loginErrorTimes)

        // Then use the correct password
        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        // The error counter has been reset
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun login_userNotFound_returnsStatus() {
        val res = passportService.login(
            PassportLoginRequest(tenantId, "no-such-user", plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.USER_NOT_FOUND, res.status)
        assertNull(res.userInfo)
        assertNull(res.loginErrorTimes)
    }

    @Test
    fun login_inactiveUser_returnsStatus() {
        val res = passportService.login(
            PassportLoginRequest(tenantId, inactiveUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.INACTIVE, res.status)
        assertNull(res.userInfo)
        // Should not consume error attempts (account is inactive, never enters the password check path)
        assertEquals(0, userAccountDao.get(inactiveUserId)?.loginErrorTimes)
    }

    @Test
    fun login_withLoginIp_updatesLastLoginInfo() {
        val ip = 0x7F000001L // 127.0.0.1 as Long
        val res = passportService.login(
            PassportLoginRequest(
                tenantId = tenantId,
                username = activeUsername,
                plainPassword = plainPassword,
                loginIp = ip,
            )
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        val po = assertNotNull(userAccountDao.get(activeUserId))
        assertEquals(ip, po.lastLoginIp)
        assertNotNull(po.lastLoginTime)
    }

    @Test
    fun login_withoutLoginIp_doesNotUpdateLastLoginInfo() {
        // Capture lastLoginIp before login (fixture sets it to null)
        val before = userAccountDao.get(activeUserId)?.lastLoginIp
        passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        val after = userAccountDao.get(activeUserId)?.lastLoginIp
        assertEquals(before, after)
        assertTrue(after == null)
    }

    @Test
    fun logout_writesLastLogoutTime() {
        // last_logout_time is null in the fixture
        assertNull(userAccountDao.get(activeUserId)?.lastLogoutTime)

        val ok = passportService.logout(activeUserId)
        assertTrue(ok)
        val after = userAccountDao.get(activeUserId)
        assertNotNull(after?.lastLogoutTime)
    }

    @Test
    fun logout_unknownUser_returnsFalse() {
        val ok = passportService.logout("00000000-0000-0000-0000-000000000000")
        assertFalse(ok)
    }

    // ---- verifyPassword ------------------------------------------------------------------

    @Test
    fun verifyPassword_correct_returnsTrue() {
        val ok = passportService.verifyPassword(VerifyPasswordRequest(activeUserId, plainPassword))
        assertTrue(ok)
        // Does not consume error attempts
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun verifyPassword_wrong_returnsFalseAndDoesNotIncrementCounter() {
        val ok = passportService.verifyPassword(VerifyPasswordRequest(activeUserId, "wrong"))
        assertFalse(ok)
        // Key point: does not consume error attempts (unlike login)
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun verifyPassword_unknownUser_returnsFalse() {
        val ok = passportService.verifyPassword(
            VerifyPasswordRequest("00000000-0000-0000-0000-000000000000", plainPassword)
        )
        assertFalse(ok)
    }

    // ---- changePassword ------------------------------------------------------------------

    @Test
    fun changePassword_success_updatesAndIsImmediatelyUsable() {
        val newPlain = "brand-new-password-456"
        val res = passportService.changePassword(
            ChangePasswordRequest(activeUserId, plainPassword, newPlain)
        )
        assertEquals(ChangePasswordResultEnum.SUCCESS, res)

        // Old password no longer works
        assertFalse(passportService.verifyPassword(VerifyPasswordRequest(activeUserId, plainPassword)))
        // New password works
        assertTrue(passportService.verifyPassword(VerifyPasswordRequest(activeUserId, newPlain)))
    }

    @Test
    fun changePassword_wrongOldPassword_returnsErrorAndDoesNotWrite() {
        val originalHash = userAccountDao.get(activeUserId)?.loginPassword
        val res = passportService.changePassword(
            ChangePasswordRequest(activeUserId, "wrong-old", "doesnt-matter")
        )
        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, res)
        // The hash in the database is unchanged
        assertEquals(originalHash, userAccountDao.get(activeUserId)?.loginPassword)
    }

    @Test
    fun changePassword_unknownUser_returnsUserNotFound() {
        val res = passportService.changePassword(
            ChangePasswordRequest(
                userId = "00000000-0000-0000-0000-000000000000",
                oldPlainPassword = "anything",
                newPlainPassword = "anything-new",
            )
        )
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, res)
    }

    // ---- OTP login gate ------------------------------------------------------------------

    @Test
    fun login_noOtpEnabled_authCodeIgnored() {
        // User has not enabled OTP (authentication_key=null), authCode is optional
        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword, authCode = 999999L)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
    }

    @Test
    fun login_otpEnabledButCodeMissing_returnsOtpRequired() {
        // Enable OTP
        userAccountService.resetAuthKey(activeUserId, activeUsername, "kudos")
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.OTP_REQUIRED, res.status)
        // Note: OTP_REQUIRED does not increment the error counter
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun login_otpEnabledWrongCode_returnsOtpWrongAndIncrementsCounter() {
        userAccountService.resetAuthKey(activeUserId, activeUsername, "kudos")
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword, authCode = 0L)
        )
        assertEquals(PassportLoginStatusEnum.OTP_WRONG, res.status)
        assertEquals(1, res.loginErrorTimes)
        assertEquals(1, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun login_otpEnabledCorrectCode_returnsSuccess() {
        val setup = assertNotNull(userAccountService.resetAuthKey(activeUserId, activeUsername, "kudos"))
        userAccountHashCache.reloadAll(clear = true)

        val code = currentTotpCode(setup.secret)
        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword, authCode = code)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        // Error counter is cleared once all checks pass
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    /**
     * Compute the 6-digit TOTP code for the current 30s window (RFC 6238 dynamic truncation).
     *
     * Same algorithm as [io.kudos.base.security.GoogleAuthenticator.verifyCode], which is
     * `internal` and not reachable across modules, so it is reimplemented here.
     */
    private fun currentTotpCode(base32Secret: String): Long {
        val key = Base32().decode(base32Secret)
        var t = System.currentTimeMillis() / 1000L / 30L
        val data = ByteArray(8)
        var i = 8
        while (i-- > 0) {
            data[i] = t.toByte()
            t = t ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = (hash[hash.size - 1].toInt() and 0xF)
        val truncated =
            ((hash[offset].toInt() and 0x7F).toLong() shl 24) or
                ((hash[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((hash[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (hash[offset + 3].toInt() and 0xFF).toLong()
        return truncated % 1_000_000L
    }

    // ---- Account freeze gate ------------------------------------------------------------

    @Test
    fun login_currentlyFrozen_returnsAccountFrozen() {
        userAccountService.freezeAccount(
            activeUserId, "manual",
            freezeTitle = "Under maintenance",
            freezeContent = null,
            freezeStartTime = null,
            freezeEndTime = java.time.LocalDateTime.now().plusHours(1),
        )
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.ACCOUNT_FROZEN, res.status)
        assertEquals("Under maintenance", res.message) // Freeze title is passed through
        // Does not affect the error counter (freeze takes precedence over password check)
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun login_frozenButOutsideWindow_allowsLogin() {
        userAccountService.freezeAccount(
            activeUserId, "manual", "Expired freeze", null,
            freezeStartTime = java.time.LocalDateTime.now().minusDays(2),
            freezeEndTime = java.time.LocalDateTime.now().minusDays(1),
        )
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
    }

    @Test
    fun login_frozenStartInFuture_allowsLogin() {
        userAccountService.freezeAccount(
            activeUserId, "scheduled", "Freeze starts tomorrow", null,
            freezeStartTime = java.time.LocalDateTime.now().plusDays(1),
            freezeEndTime = java.time.LocalDateTime.now().plusDays(2),
        )
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
    }

    @Test
    fun login_permanentlyFrozen_returnsAccountFrozen() {
        userAccountService.freezeAccount(
            activeUserId, "admin", "Account banned", "Violation of ToS",
            freezeStartTime = null, freezeEndTime = null,
        )
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.ACCOUNT_FROZEN, res.status)
    }

    // ---- Brute-force lockout gate --------------------------------------------------------

    @Test
    fun login_reachingErrorThreshold_locksAccountAndRejectsCorrectPassword() {
        // 4 failures stay below the default threshold (5) and answer WRONG_PASSWORD
        repeat(4) { i ->
            val res = passportService.login(PassportLoginRequest(tenantId, activeUsername, "wrong-$i"))
            assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        }

        // The 5th failure crosses the threshold: LOCKED + the account is auto-frozen
        val fifth = passportService.login(PassportLoginRequest(tenantId, activeUsername, "wrong-4"))
        assertEquals(PassportLoginStatusEnum.LOCKED, fifth.status)
        assertEquals(5, fifth.loginErrorTimes)
        val po = assertNotNull(userAccountDao.get(activeUserId))
        assertEquals(PassportService.LOGIN_LOCK_FREEZE_TYPE, po.freezeType)
        assertNotNull(po.freezeEndTime)

        // While the lock window is active even the CORRECT password is rejected with LOCKED
        userAccountHashCache.reloadAll(clear = true)
        val locked = passportService.login(PassportLoginRequest(tenantId, activeUsername, plainPassword))
        assertEquals(PassportLoginStatusEnum.LOCKED, locked.status)
        // The locked attempt must not consume another error count
        assertEquals(5, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun login_afterLockWindowExpiresAndSuccess_resetsCounterAndLockIsGone() {
        // Arm the lock by reaching the threshold
        repeat(5) { i ->
            passportService.login(PassportLoginRequest(tenantId, activeUsername, "wrong-$i"))
        }
        assertEquals(PassportService.LOGIN_LOCK_FREEZE_TYPE, userAccountDao.get(activeUserId)?.freezeType)

        // Simulate window expiry by moving freeze_end_time into the past
        userAccountDao.updateProperties(
            activeUserId,
            mapOf(UserAccount::freezeEndTime.name to java.time.LocalDateTime.now().minusMinutes(1)),
        )
        userAccountHashCache.reloadAll(clear = true)

        // Correct password now passes and resets the error counter
        val res = passportService.login(PassportLoginRequest(tenantId, activeUsername, plainPassword))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun login_afterUnfreeze_allowsLogin() {
        userAccountService.freezeAccount(activeUserId, "manual", "t", "c", null, null)
        userAccountHashCache.reloadAll(clear = true)
        assertEquals(
            PassportLoginStatusEnum.ACCOUNT_FROZEN,
            passportService.login(PassportLoginRequest(tenantId, activeUsername, plainPassword)).status
        )

        userAccountService.unfreezeAccount(activeUserId)
        userAccountHashCache.reloadAll(clear = true)
        assertEquals(
            PassportLoginStatusEnum.SUCCESS,
            passportService.login(PassportLoginRequest(tenantId, activeUsername, plainPassword)).status
        )
    }
}
