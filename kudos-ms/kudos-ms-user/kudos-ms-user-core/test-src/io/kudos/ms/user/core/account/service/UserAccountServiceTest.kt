package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.*

/**
 * junit test for UserAccountService
 *
 * 测试数据来源：`UserAccountServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userAccountService: IUserAccountService

    @Test
    fun getUserByTenantIdAndUsername() {
        val tenantId = "svc-tenant-user-test-1-3iZR7Pv6"
        val username = "svc-user-test-1-3iZR7Pv6"
        val cacheItem = userAccountService.getUserByTenantIdAndUsername(tenantId, username)
        assertNotNull(cacheItem)
        assertTrue(cacheItem.username == username)
        
        // 测试不存在的用户
        val notExist = userAccountService.getUserByTenantIdAndUsername(tenantId, "non-existent")
        assertNull(notExist)
    }

    @Test
    fun getUserRecord() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val record = userAccountService.getUserRecord(id)
        assertNotNull(record)
        assertTrue(record.username == "svc-user-test-1-3iZR7Pv6")
        
        // 测试不存在的用户
        val notExist = userAccountService.getUserRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getUsersByTenantId() {
        val tenantId = "svc-tenant-user-test-1-3iZR7Pv6"
        val users = userAccountService.getUsersByTenantId(tenantId)
        assertTrue(users.size >= 3)
        assertTrue(users.any { it.username == "svc-user-test-1-3iZR7Pv6" })
    }

    @Test
    fun getUsersByOrgId() {
        val orgId = "a970f8c0-0000-0000-0000-000000000020"
        val users = userAccountService.getUsersByOrgId(orgId)
        assertTrue(users.size >= 2)
        assertTrue(users.any { it.username == "svc-user-test-1-3iZR7Pv6" })
        assertTrue(users.any { it.username == "svc-user-test-2-3iZR7Pv6" })
    }

    @Test
    fun updateActive() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        // 先设置为false
        assertTrue(userAccountService.updateActive(id, false))
        var user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        assertFalse(user.active == true)
        
        // 再设置为true
        assertTrue(userAccountService.updateActive(id, true))
        user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.active == true)
    }

    @Test
    fun resetPassword() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val newPassword = "new-password-123"
        assertTrue(userAccountService.resetPassword(id, newPassword))
        val user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        // 验证登录错误次数被重置
        assertTrue(user.loginErrorTimes == 0)
    }

    @Test
    fun resetSecurityPassword() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val newPassword = "new-security-pwd-123"
        assertTrue(userAccountService.resetSecurityPassword(id, newPassword))
        val user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        // 验证安全密码错误次数被重置
        assertTrue(user.securityPasswordErrorTimes == 0)
    }

    @Test
    fun updateLastLoginInfo() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val loginIp = 192168001001L
        val loginTime = LocalDateTime.now()
        assertTrue(userAccountService.updateLastLoginInfo(id, loginIp, loginTime))
        val user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.lastLoginIp == loginIp)
        // 验证登录错误次数被重置
        assertTrue(user.loginErrorTimes == 0)
    }

    @Test
    fun updateLastLogoutInfo() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val logoutTime = LocalDateTime.now()
        assertTrue(userAccountService.updateLastLogoutInfo(id, logoutTime))
        val user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        assertNotNull(user.lastLogoutTime)
    }

    @Test
    fun incrementLoginErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        val userBefore = userAccountService.getUserRecord(id)
        val errorTimesBefore = userBefore?.loginErrorTimes ?: 0
        
        assertTrue(userAccountService.incrementLoginErrorTimes(id))
        val userAfter = userAccountService.getUserRecord(id)
        assertNotNull(userAfter)
        assertTrue((userAfter.loginErrorTimes ?: 0) == errorTimesBefore + 1)
    }

    @Test
    fun resetLoginErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        // 先增加错误次数
        userAccountService.incrementLoginErrorTimes(id)
        
        // 然后重置
        assertTrue(userAccountService.resetLoginErrorTimes(id))
        val user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.loginErrorTimes == 0)
    }

    @Test
    fun incrementSecurityPasswordErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        val userBefore = userAccountService.getUserRecord(id)
        val errorTimesBefore = userBefore?.securityPasswordErrorTimes ?: 0
        
        assertTrue(userAccountService.incrementSecurityPasswordErrorTimes(id))
        val userAfter = userAccountService.getUserRecord(id)
        assertNotNull(userAfter)
        assertTrue((userAfter.securityPasswordErrorTimes ?: 0) == errorTimesBefore + 1)
    }

    @Test
    fun resetSecurityPasswordErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        // 先增加错误次数
        userAccountService.incrementSecurityPasswordErrorTimes(id)

        // 然后重置
        assertTrue(userAccountService.resetSecurityPasswordErrorTimes(id))
        val user = userAccountService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.securityPasswordErrorTimes == 0)
    }

    /** resetAuthKey 生成新 secret 并落库；返回的 otpauth URL 形态合法。 */
    @Test
    fun resetAuthKey_storesSecretAndReturnsOtpauthUrl() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        val setup = userAccountService.resetAuthKey(id, "alice", "kudos")
        assertNotNull(setup)
        assertTrue(setup.secret.isNotBlank())
        assertTrue(setup.otpauthUrl.startsWith("otpauth://totp/"))
        assertTrue(setup.otpauthUrl.contains("secret=${setup.secret}"))
        assertTrue(setup.otpauthUrl.contains("issuer=kudos"))
        // 数据库里应该存了 secret
        val user = userAccountService.get(id)
        assertNotNull(user)
        assertEquals(setup.secret, user.authenticationKey)
    }

    /** resetAuthKey 对不存在的用户应当返回 null（dao.update 失败）。 */
    @Test
    fun resetAuthKey_unknownUser_returnsNull() {
        val res = userAccountService.resetAuthKey(
            "00000000-0000-0000-0000-000000000000", "alice", "kudos"
        )
        assertNull(res)
    }

    /** cleanAuthKey 清掉已有 secret。 */
    @Test
    fun cleanAuthKey_clearsExistingSecret() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        // 先种一个 secret
        userAccountService.resetAuthKey(id, "alice", "kudos")
        assertNotNull(userAccountService.get(id)?.authenticationKey)

        assertTrue(userAccountService.cleanAuthKey(id))
        assertNull(userAccountService.get(id)?.authenticationKey)
    }

    /** verifyAuthCode 在未启用 OTP（authenticationKey=null）时返回 false。 */
    @Test
    fun verifyAuthCode_noKey_returnsFalse() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        userAccountService.cleanAuthKey(id)
        assertFalse(userAccountService.verifyAuthCode(id, 123456L))
    }

    /** verifyAuthCode 对一个明显错误的验证码返回 false（不抛异常）。 */
    @Test
    fun verifyAuthCode_wrongCode_returnsFalse() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        userAccountService.resetAuthKey(id, "alice", "kudos")
        // 0 几乎不可能匹配当前时间窗的 TOTP
        assertFalse(userAccountService.verifyAuthCode(id, 0L))
    }

    /** freezeAccount 写入 6 列。 */
    @Test
    fun freezeAccount_writesAllSixFields() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        val start = LocalDateTime.now().minusMinutes(1)
        val end = LocalDateTime.now().plusHours(1)
        val ok = userAccountService.freezeAccount(
            id = id,
            freezeType = "manual",
            freezeTitle = "test-freeze",
            freezeContent = "from UserAccountServiceTest",
            freezeStartTime = start,
            freezeEndTime = end,
        )
        assertTrue(ok)
        val po = assertNotNull(userAccountService.get(id))
        assertEquals("manual", po.freezeType)
        assertEquals("test-freeze", po.freezeTitle)
        assertEquals("from UserAccountServiceTest", po.freezeContent)
        assertEquals(start, po.freezeStartTime)
        assertEquals(end, po.freezeEndTime)
        assertNotNull(po.freezeTime)
    }

    /** unfreezeAccount 清空全部 6 列。 */
    @Test
    fun unfreezeAccount_clearsAllSixFields() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        userAccountService.freezeAccount(id, "manual", "t", "c", null, null)
        assertNotNull(userAccountService.get(id)?.freezeType)

        assertTrue(userAccountService.unfreezeAccount(id))
        val po = assertNotNull(userAccountService.get(id))
        assertNull(po.freezeType)
        assertNull(po.freezeTime)
        assertNull(po.freezeStartTime)
        assertNull(po.freezeEndTime)
        assertNull(po.freezeTitle)
        assertNull(po.freezeContent)
    }

    /** cleanExpiredFreezes 只清理 freeze_end_time < now 的记录；永久冻结和未来生效的保留。 */
    @Test
    fun cleanExpiredFreezes_clearsExpiredOnly_keepsPermanentAndFuture() {
        val expiredId = "a970f8c0-0000-0000-0000-000000000017"
        val permanentId = "a970f8c0-0000-0000-0000-000000000018"
        val futureId = "a970f8c0-0000-0000-0000-000000000016"

        userAccountService.freezeAccount(
            expiredId, "manual", "expired", "c",
            freezeStartTime = LocalDateTime.now().minusDays(2),
            freezeEndTime = LocalDateTime.now().minusDays(1),
        )
        userAccountService.freezeAccount(
            permanentId, "admin", "permanent", "c",
            freezeStartTime = null, freezeEndTime = null,
        )
        userAccountService.freezeAccount(
            futureId, "scheduled", "future", "c",
            freezeStartTime = LocalDateTime.now().plusDays(1),
            freezeEndTime = LocalDateTime.now().plusDays(2),
        )

        val cleared = userAccountService.cleanExpiredFreezes()
        assertTrue(cleared >= 1, "至少清掉过期的那一条")

        // 过期的被清掉
        assertNull(userAccountService.get(expiredId)?.freezeType)
        // 永久冻结保留
        assertEquals("admin", userAccountService.get(permanentId)?.freezeType)
        // 未来才生效的也保留（freezeEndTime 在未来）
        assertEquals("scheduled", userAccountService.get(futureId)?.freezeType)
    }

    /** cleanExpiredFreezes 在没有过期记录时返回 0、不抛异常。 */
    @Test
    fun cleanExpiredFreezes_noExpired_returnsZero() {
        val cleared = userAccountService.cleanExpiredFreezes()
        assertEquals(0, cleared)
    }
}
