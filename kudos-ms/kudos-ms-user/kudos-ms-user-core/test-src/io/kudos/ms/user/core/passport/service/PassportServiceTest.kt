package io.kudos.ms.user.core.passport.service

import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeEach
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
}
