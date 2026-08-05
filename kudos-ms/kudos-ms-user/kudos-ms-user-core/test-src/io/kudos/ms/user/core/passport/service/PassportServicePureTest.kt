package io.kudos.ms.user.core.passport.service

import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.service.impl.PassportService
import org.apache.commons.codec.binary.Base32
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.time.LocalDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [PassportService] login/verify/change pipeline.
 *
 * Collaborators ([IUserAccountService], [UserAccountDao]) are Mockito mocks, so the full branch
 * matrix of [PassportService.login] and the private `registerLoginFailure` helper is exercised
 * without a Spring container or database. `PasswordKit` and `GoogleAuthenticator` are the real
 * (pure) implementations from kudos-base.
 *
 * The `@Value` lock-config fields (`maxLoginErrorTimes`, `loginLockMinutes`) keep their Kotlin
 * default initializers (5 / 30) on direct instantiation; tests that need other thresholds override
 * them by reflection.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportServicePureTest {

    private val userAccountService = mock(IUserAccountService::class.java)
    private val userAccountDao = mock(UserAccountDao::class.java)
    private val service = PassportService(userAccountService, userAccountDao)

    private val plain = "secret-pwd-123"
    private val hash = PasswordKit.hash(plain, strength = 4)

    private fun entry(
        id: String = "u1",
        username: String? = "alice",
        tenantId: String? = "t1",
        loginPassword: String? = hash,
        active: Boolean? = true,
        authenticationKey: String? = null,
        loginErrorTimes: Int? = 0,
        freezeType: String? = null,
        freezeStartTime: LocalDateTime? = null,
        freezeEndTime: LocalDateTime? = null,
        freezeTitle: String? = null,
        orgId: String? = "org1",
    ) = UserAccountCacheEntry(
        id = id,
        username = username,
        tenantId = tenantId,
        loginPassword = loginPassword,
        securityPassword = null,
        accountTypeDictCode = "10",
        accountStatusDictCode = null,
        defaultLocale = "en",
        defaultTimezone = "UTC",
        defaultCurrency = "USD",
        lastLoginTime = null,
        lastLoginIp = null,
        lastLogoutTime = null,
        loginErrorTimes = loginErrorTimes,
        securityPasswordErrorTimes = null,
        sessionKey = null,
        authenticationKey = authenticationKey,
        orgId = orgId,
        supervisorId = null,
        remark = null,
        freezeType = freezeType,
        freezeStartTime = freezeStartTime,
        freezeEndTime = freezeEndTime,
        freezeTitle = freezeTitle,
        active = active,
        builtIn = false,
        createUserId = null,
        createUserName = null,
        createTime = null,
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
    )

    private fun setMaxErrorTimes(value: Int) {
        val f = PassportService::class.java.getDeclaredField("maxLoginErrorTimes")
        f.isAccessible = true
        f.setInt(service, value)
    }

    private fun setLockMinutes(value: Long) {
        val f = PassportService::class.java.getDeclaredField("loginLockMinutes")
        f.isAccessible = true
        f.setLong(service, value)
    }

    private fun stub(tenantId: String, username: String, e: UserAccountCacheEntry?) {
        whenCalled(userAccountService.getUserByTenantIdAndUsername(tenantId, username)).thenReturn(e)
    }

    // Mockito + Kotlin matcher helpers: matchers return null, which a non-null Kotlin parameter
    // rejects with an intrinsics NPE; the `?: fallback` keeps the matcher side effect while
    // returning a non-null placeholder the call site accepts.
    private fun eqStr(value: String): String = eq(value) ?: value
    private fun eqLong(value: Long): Long = eq(value) ?: value
    private fun anyDateTime(): LocalDateTime = any(LocalDateTime::class.java) ?: LocalDateTime.now()

    // ---- login: lookup / status gates -----------------------------------------------------

    @Test
    fun login_userNotFound() {
        stub("t1", "ghost", null)
        val res = service.login(PassportLoginRequest("t1", "ghost", plain))
        assertEquals(PassportLoginStatusEnum.USER_NOT_FOUND, res.status)
        assertNull(res.userInfo)
        verify(userAccountService, never()).incrementLoginErrorTimes(anyString())
    }

    @Test
    fun login_inactiveAccount() {
        stub("t1", "alice", entry(active = false))
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.INACTIVE, res.status)
        assertNull(res.userInfo)
    }

    @Test
    fun login_activeNull_treatedAsInactive() {
        stub("t1", "alice", entry(active = null))
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.INACTIVE, res.status)
    }

    // ---- login: freeze gate ----------------------------------------------------------------

    @Test
    fun login_frozenManual_returnsAccountFrozenWithTitle() {
        stub(
            "t1", "alice",
            entry(
                freezeType = "manual",
                freezeTitle = "Under maintenance",
                freezeStartTime = null,
                freezeEndTime = LocalDateTime.now().plusHours(1),
            )
        )
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.ACCOUNT_FROZEN, res.status)
        assertEquals("Under maintenance", res.message)
        verify(userAccountService, never()).incrementLoginErrorTimes(anyString())
    }

    @Test
    fun login_frozenWithAutoLockType_returnsLocked() {
        stub(
            "t1", "alice",
            entry(
                freezeType = PassportService.LOGIN_LOCK_FREEZE_TYPE,
                loginErrorTimes = 7,
                freezeStartTime = null,
                freezeEndTime = LocalDateTime.now().plusMinutes(30),
            )
        )
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        assertEquals(7, res.loginErrorTimes)
    }

    @Test
    fun login_frozenButWindowExpired_proceedsToPasswordCheck() {
        stub(
            "t1", "alice",
            entry(
                freezeType = "manual",
                freezeStartTime = LocalDateTime.now().minusDays(2),
                freezeEndTime = LocalDateTime.now().minusDays(1),
            )
        )
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
    }

    // ---- login: password verification ------------------------------------------------------

    @Test
    fun login_wrongPassword_belowThreshold_returnsWrongPassword() {
        stub("t1", "alice", entry(loginErrorTimes = 0))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 1))
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        assertEquals(1, res.loginErrorTimes)
        verify(userAccountService).incrementLoginErrorTimes("u1")
        verify(userAccountService, never()).freezeAccount(anyString(), anyString(), any(), any(), any(), any())
    }

    @Test
    fun login_wrongPassword_getUserRecordNull_fallsBackToCachePlusOne() {
        // getUserRecord returns null -> accumulated falls back to (cache count + 1)
        stub("t1", "alice", entry(loginErrorTimes = 2))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(null)
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        assertEquals(3, res.loginErrorTimes)
    }

    @Test
    fun login_wrongPassword_getUserRecordNullAndCacheCountNull_fallsBackToOne() {
        stub("t1", "alice", entry(loginErrorTimes = null))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(null)
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        assertEquals(1, res.loginErrorTimes)
    }

    @Test
    fun login_wrongPassword_reachingThreshold_locksAccount() {
        stub("t1", "alice", entry(loginErrorTimes = 4, freezeType = null))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 5))
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        assertEquals(5, res.loginErrorTimes)
        verify(userAccountService).freezeAccount(
            eqStr("u1"), eqStr(PassportService.LOGIN_LOCK_FREEZE_TYPE), anyString(), anyString(), isNull(), any()
        )
    }

    @Test
    fun login_wrongPassword_reachingThreshold_foreignFreezeNotClobbered() {
        // An expired foreign freeze: the freeze gate lets the login through (window expired), but
        // canArmLoginLock("admin")=false so the auto-lock must not overwrite the admin freeze record.
        stub(
            "t1", "alice",
            entry(
                loginErrorTimes = 4,
                freezeType = "admin",
                freezeStartTime = LocalDateTime.now().minusDays(2),
                freezeEndTime = LocalDateTime.now().minusDays(1),
            )
        )
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 5))
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        // shouldLock=true so LOCKED is still returned, but freezeAccount must NOT be called
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        verify(userAccountService, never()).freezeAccount(anyString(), anyString(), any(), any(), any(), any())
    }

    @Test
    fun login_wrongPassword_lockMinutesNonPositive_freezesWithNullEnd() {
        setLockMinutes(0)
        stub("t1", "alice", entry(loginErrorTimes = 4, freezeType = null))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 5))
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        // lockEnd is null -> freezeEndTime passed as null (permanent until manual unfreeze)
        verify(userAccountService).freezeAccount(
            eqStr("u1"), eqStr(PassportService.LOGIN_LOCK_FREEZE_TYPE), anyString(), anyString(), isNull(), isNull()
        )
    }

    @Test
    fun login_wrongPassword_thresholdDisabled_neverLocks() {
        setMaxErrorTimes(0)
        stub("t1", "alice", entry(loginErrorTimes = 99, freezeType = null))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 100))
        val res = service.login(PassportLoginRequest("t1", "alice", "bad"))
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, res.status)
        verify(userAccountService, never()).freezeAccount(anyString(), anyString(), any(), any(), any(), any())
    }

    // ---- login: success path ---------------------------------------------------------------

    @Test
    fun login_success_noIp_resetsCounterAndReturnsUserInfo() {
        stub("t1", "alice", entry())
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        val info = assertNotNull(res.userInfo)
        assertEquals("u1", info.id)
        assertEquals("alice", info.username)
        assertEquals("t1", info.tenantId)
        assertEquals("org1", info.orgId)
        verify(userAccountService).resetLoginErrorTimes("u1")
        verify(userAccountService, never()).updateLastLoginInfo(anyString(), anyLong(), anyDateTime())
    }

    @Test
    fun login_success_withIp_updatesLastLoginInfo() {
        stub("t1", "alice", entry())
        val ip = 0x7F000001L
        val res = service.login(PassportLoginRequest("t1", "alice", plain, loginIp = ip))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        verify(userAccountService).resetLoginErrorTimes("u1")
        verify(userAccountService).updateLastLoginInfo(eqStr("u1"), eqLong(ip), anyDateTime())
    }

    @Test
    fun login_success_nullUsernameAndTenant_orEmptyApplied() {
        stub("t1", "alice", entry(username = null, tenantId = null))
        val res = service.login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        val info = assertNotNull(res.userInfo)
        assertEquals("", info.username)
        assertEquals("", info.tenantId)
    }

    // ---- login: OTP gate -------------------------------------------------------------------

    @Test
    fun login_otpEnabled_codeMissing_returnsOtpRequired() {
        stub("t1", "alice", entry(authenticationKey = "JBSWY3DPEHPK3PXP"))
        val res = service.login(PassportLoginRequest("t1", "alice", plain, authCode = null))
        assertEquals(PassportLoginStatusEnum.OTP_REQUIRED, res.status)
        // OTP_REQUIRED must not consume the error counter
        verify(userAccountService, never()).incrementLoginErrorTimes(anyString())
        verify(userAccountService, never()).resetLoginErrorTimes(anyString())
    }

    @Test
    fun login_otpEnabled_wrongCode_returnsOtpWrongAndIncrements() {
        stub("t1", "alice", entry(authenticationKey = "JBSWY3DPEHPK3PXP", loginErrorTimes = 0))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 1))
        val res = service.login(PassportLoginRequest("t1", "alice", plain, authCode = 1L))
        assertEquals(PassportLoginStatusEnum.OTP_WRONG, res.status)
        assertEquals(1, res.loginErrorTimes)
        verify(userAccountService).incrementLoginErrorTimes("u1")
    }

    @Test
    fun login_otpEnabled_wrongCode_reachingThreshold_returnsLocked() {
        stub("t1", "alice", entry(authenticationKey = "JBSWY3DPEHPK3PXP", loginErrorTimes = 4, freezeType = null))
        whenCalled(userAccountService.getUserRecord("u1")).thenReturn(UserAccountRow(id = "u1", loginErrorTimes = 5))
        val res = service.login(PassportLoginRequest("t1", "alice", plain, authCode = 1L))
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        verify(userAccountService).freezeAccount(
            eqStr("u1"), eqStr(PassportService.LOGIN_LOCK_FREEZE_TYPE), anyString(), anyString(), isNull(), any()
        )
    }

    @Test
    fun login_otpEnabled_correctCode_returnsSuccess() {
        val secret = "JBSWY3DPEHPK3PXP"
        stub("t1", "alice", entry(authenticationKey = secret))
        val code = currentTotpCode(secret)
        val res = service.login(PassportLoginRequest("t1", "alice", plain, authCode = code))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        verify(userAccountService).resetLoginErrorTimes("u1")
    }

    @Test
    fun login_otpKeyBlank_treatedAsDisabled() {
        // authenticationKey blank (not null) -> isNullOrBlank true -> OTP skipped, success even with bogus code
        stub("t1", "alice", entry(authenticationKey = "   "))
        val res = service.login(PassportLoginRequest("t1", "alice", plain, authCode = 123456L))
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
    }

    // ---- logout ----------------------------------------------------------------------------

    @Test
    fun logout_success() {
        whenCalled(userAccountService.updateLastLogoutInfo(eqStr("u1"), anyDateTime())).thenReturn(true)
        assertTrue(service.logout("u1"))
        verify(userAccountService).updateLastLogoutInfo(eqStr("u1"), anyDateTime())
    }

    @Test
    fun logout_failure() {
        whenCalled(userAccountService.updateLastLogoutInfo(eqStr("u-none"), anyDateTime())).thenReturn(false)
        assertFalse(service.logout("u-none"))
    }

    // ---- verifyPassword / verifySecurityPassword -------------------------------------------

    @Test
    fun verifyPassword_correct() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.loginPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        assertTrue(service.verifyPassword(VerifyPasswordRequest("u1", plain)))
    }

    @Test
    fun verifyPassword_wrong() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.loginPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        assertFalse(service.verifyPassword(VerifyPasswordRequest("u1", "nope")))
    }

    @Test
    fun verifyPassword_userNotFound_returnsFalse() {
        whenCalled(userAccountDao.get("ghost")).thenReturn(null)
        assertFalse(service.verifyPassword(VerifyPasswordRequest("ghost", plain)))
    }

    @Test
    fun verifySecurityPassword_correct() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.securityPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        assertTrue(service.verifySecurityPassword(VerifyPasswordRequest("u1", plain)))
    }

    @Test
    fun verifySecurityPassword_userNotFound_returnsFalse() {
        whenCalled(userAccountDao.get("ghost")).thenReturn(null)
        assertFalse(service.verifySecurityPassword(VerifyPasswordRequest("ghost", plain)))
    }

    @Test
    fun verifySecurityPassword_nullStoredHash_returnsFalse() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.securityPassword).thenReturn(null)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        assertFalse(service.verifySecurityPassword(VerifyPasswordRequest("u1", plain)))
    }

    // ---- changePassword --------------------------------------------------------------------

    @Test
    fun changePassword_userNotFound() {
        whenCalled(userAccountDao.get("ghost")).thenReturn(null)
        val res = service.changePassword(ChangePasswordRequest("ghost", "old", "new"))
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, res)
        verify(userAccountService, never()).resetPassword(anyString(), anyString())
    }

    @Test
    fun changePassword_oldWrong() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.loginPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        val res = service.changePassword(ChangePasswordRequest("u1", "wrong-old", "new"))
        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, res)
        verify(userAccountService, never()).resetPassword(anyString(), anyString())
    }

    @Test
    fun changePassword_success() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.loginPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        val res = service.changePassword(ChangePasswordRequest("u1", plain, "new-pwd"))
        assertEquals(ChangePasswordResultEnum.SUCCESS, res)
        verify(userAccountService).resetPassword("u1", "new-pwd")
    }

    // ---- changeSecurityPassword ------------------------------------------------------------

    @Test
    fun changeSecurityPassword_userNotFound() {
        whenCalled(userAccountDao.get("ghost")).thenReturn(null)
        val res = service.changeSecurityPassword(ChangePasswordRequest("ghost", "old", "new"))
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, res)
        verify(userAccountService, never()).resetSecurityPassword(anyString(), anyString())
    }

    @Test
    fun changeSecurityPassword_oldWrong() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.securityPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        val res = service.changeSecurityPassword(ChangePasswordRequest("u1", "wrong-old", "new"))
        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, res)
        verify(userAccountService, never()).resetSecurityPassword(anyString(), anyString())
    }

    @Test
    fun changeSecurityPassword_success() {
        val po = mock(UserAccount::class.java)
        whenCalled(po.securityPassword).thenReturn(hash)
        whenCalled(userAccountDao.get("u1")).thenReturn(po)
        val res = service.changeSecurityPassword(ChangePasswordRequest("u1", plain, "new-spwd"))
        assertEquals(ChangePasswordResultEnum.SUCCESS, res)
        verify(userAccountService).resetSecurityPassword("u1", "new-spwd")
    }

    /**
     * Compute the 6-digit TOTP code for the current 30s window (RFC 6238 dynamic truncation),
     * mirroring [io.kudos.base.security.GoogleAuthenticator.verifyCode] which is `internal`.
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
        val h = mac.doFinal(data)
        val offset = (h[h.size - 1].toInt() and 0xF)
        val truncated =
            ((h[offset].toInt() and 0x7F).toLong() shl 24) or
                ((h[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((h[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (h[offset + 3].toInt() and 0xFF).toLong()
        return truncated % 1_000_000L
    }
}
