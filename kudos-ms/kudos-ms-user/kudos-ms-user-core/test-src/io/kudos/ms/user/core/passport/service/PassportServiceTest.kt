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
 * 测试数据来源：`PassportServiceTest.sql`
 *
 * 用户表里种 active=true 用户 + active=false 用户各一名。`@BeforeEach` 把密码就地
 * BCrypt 哈希（cost=4 加速测试），随后通过 [IPassportService.login] 走完整链路。
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
        // 计算一次 BCrypt 哈希（cost=4 用于测试，~5ms），更新两条种子用户
        val hash = PasswordKit.hash(plainPassword, strength = 4)
        userAccountDao.updateProperties(activeUserId, mapOf(UserAccount::loginPassword.name to hash))
        userAccountDao.updateProperties(inactiveUserId, mapOf(UserAccount::loginPassword.name to hash))
        // 清零错误计数，避免上轮残留
        userAccountDao.updateProperties(activeUserId, mapOf(UserAccount::loginErrorTimes.name to 0))
        userAccountDao.updateProperties(inactiveUserId, mapOf(UserAccount::loginErrorTimes.name to 0))
        // 让缓存看到新的密码字段
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
        // 库里也应该加 1
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
        // 先错一次
        passportService.login(PassportLoginRequest(tenantId, activeUsername, "wrong"))
        assertEquals(1, userAccountDao.get(activeUserId)?.loginErrorTimes)

        // 然后用对的密码
        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
        // 错误计数已被重置
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
        // 不应该消耗错误次数（账号已停用，不进入密码校验路径）
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
        // 取登录前的 lastLoginIp（fixture 里设为 null）
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
        // fixture 里 last_logout_time 为 null
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
        // 不消耗错误次数
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    @Test
    fun verifyPassword_wrong_returnsFalseAndDoesNotIncrementCounter() {
        val ok = passportService.verifyPassword(VerifyPasswordRequest(activeUserId, "wrong"))
        assertFalse(ok)
        // 关键：不消耗错误次数（与 login 不同）
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

        // 旧密码不再有效
        assertFalse(passportService.verifyPassword(VerifyPasswordRequest(activeUserId, plainPassword)))
        // 新密码有效
        assertTrue(passportService.verifyPassword(VerifyPasswordRequest(activeUserId, newPlain)))
    }

    @Test
    fun changePassword_wrongOldPassword_returnsErrorAndDoesNotWrite() {
        val originalHash = userAccountDao.get(activeUserId)?.loginPassword
        val res = passportService.changePassword(
            ChangePasswordRequest(activeUserId, "wrong-old", "doesnt-matter")
        )
        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, res)
        // 数据库里 hash 没动
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
        // 用户没启用 OTP（authentication_key=null），authCode 是否带都行
        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword, authCode = 999999L)
        )
        assertEquals(PassportLoginStatusEnum.SUCCESS, res.status)
    }

    @Test
    fun login_otpEnabledButCodeMissing_returnsOtpRequired() {
        // 启用 OTP
        userAccountService.resetAuthKey(activeUserId, activeUsername, "kudos")
        userAccountHashCache.reloadAll(clear = true)

        val res = passportService.login(
            PassportLoginRequest(tenantId, activeUsername, plainPassword)
        )
        assertEquals(PassportLoginStatusEnum.OTP_REQUIRED, res.status)
        // 注意：OTP_REQUIRED 不增加错误计数
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
        // 验证全部通过后错误计数清零
        assertEquals(0, userAccountDao.get(activeUserId)?.loginErrorTimes)
    }

    /**
     * RFC 6238 TOTP 当前时间窗的 6 位验证码。
     *
     * 与 [io.kudos.base.security.GoogleAuthenticator.verifyCode] 同算法；后者是 internal，
     * 跨模块拿不到，于是这里复刻一份（HMAC-SHA1 + dynamic truncation + mod 1_000_000）。
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
        var truncated = (hash[offset].toInt() and 0x7F).toLong() shl 24
        truncated = truncated or ((hash[offset + 1].toInt() and 0xFF).toLong() shl 16)
        truncated = truncated or ((hash[offset + 2].toInt() and 0xFF).toLong() shl 8)
        truncated = truncated or (hash[offset + 3].toInt() and 0xFF).toLong()
        return truncated % 1_000_000L
    }
}
