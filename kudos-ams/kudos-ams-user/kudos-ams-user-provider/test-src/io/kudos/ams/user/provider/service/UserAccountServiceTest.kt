package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IUserAccountService
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
}
