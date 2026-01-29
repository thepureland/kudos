package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IAuthUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.*

/**
 * junit test for AuthUserService
 *
 * 测试数据来源：`AuthUserServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authUserService: IAuthUserService

    @Test
    fun getUserByTenantIdAndUsername() {
        val tenantId = "svc-tenant-user-test-1-3iZR7Pv6"
        val username = "svc-user-test-1-3iZR7Pv6"
        val cacheItem = authUserService.getUserByTenantIdAndUsername(tenantId, username)
        assertNotNull(cacheItem)
        assertTrue(cacheItem.username == username)
        
        // 测试不存在的用户
        val notExist = authUserService.getUserByTenantIdAndUsername(tenantId, "non-existent")
        assertNull(notExist)
    }

    @Test
    fun getUserRecord() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val record = authUserService.getUserRecord(id)
        assertNotNull(record)
        assertTrue(record.username == "svc-user-test-1-3iZR7Pv6")
        
        // 测试不存在的用户
        val notExist = authUserService.getUserRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getUsersByTenantId() {
        val tenantId = "svc-tenant-user-test-1-3iZR7Pv6"
        val users = authUserService.getUsersByTenantId(tenantId)
        assertTrue(users.size >= 3)
        assertTrue(users.any { it.username == "svc-user-test-1-3iZR7Pv6" })
    }

    @Test
    fun getUsersByDeptId() {
        val deptId = "a970f8c0-0000-0000-0000-000000000020"
        val users = authUserService.getUsersByDeptId(deptId)
        assertTrue(users.size >= 2)
        assertTrue(users.any { it.username == "svc-user-test-1-3iZR7Pv6" })
        assertTrue(users.any { it.username == "svc-user-test-2-3iZR7Pv6" })
    }

    @Test
    fun updateActive() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        // 先设置为false
        assertTrue(authUserService.updateActive(id, false))
        var user = authUserService.getUserRecord(id)
        assertNotNull(user)
        assertFalse(user.active == true)
        
        // 再设置为true
        assertTrue(authUserService.updateActive(id, true))
        user = authUserService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.active == true)
    }

    @Test
    fun resetPassword() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val newPassword = "new-password-123"
        assertTrue(authUserService.resetPassword(id, newPassword))
        val user = authUserService.getUserRecord(id)
        assertNotNull(user)
        // 验证登录错误次数被重置
        assertTrue(user.loginErrorTimes == 0)
    }

    @Test
    fun resetSecurityPassword() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val newPassword = "new-security-pwd-123"
        assertTrue(authUserService.resetSecurityPassword(id, newPassword))
        val user = authUserService.getUserRecord(id)
        assertNotNull(user)
        // 验证安全密码错误次数被重置
        assertTrue(user.securityPasswordErrorTimes == 0)
    }

    @Test
    fun updateLastLoginInfo() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val loginIp = 192168001001L
        val loginTime = LocalDateTime.now()
        assertTrue(authUserService.updateLastLoginInfo(id, loginIp, loginTime))
        val user = authUserService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.lastLoginIp == loginIp)
        // 验证登录错误次数被重置
        assertTrue(user.loginErrorTimes == 0)
    }

    @Test
    fun updateLastLogoutInfo() {
        val id = "a970f8c0-0000-0000-0000-000000000016"
        val logoutTime = LocalDateTime.now()
        assertTrue(authUserService.updateLastLogoutInfo(id, logoutTime))
        val user = authUserService.getUserRecord(id)
        assertNotNull(user)
        assertNotNull(user.lastLogoutTime)
    }

    @Test
    fun incrementLoginErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        val userBefore = authUserService.getUserRecord(id)
        val errorTimesBefore = userBefore?.loginErrorTimes ?: 0
        
        assertTrue(authUserService.incrementLoginErrorTimes(id))
        val userAfter = authUserService.getUserRecord(id)
        assertNotNull(userAfter)
        assertTrue((userAfter.loginErrorTimes ?: 0) == errorTimesBefore + 1)
    }

    @Test
    fun resetLoginErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        // 先增加错误次数
        authUserService.incrementLoginErrorTimes(id)
        
        // 然后重置
        assertTrue(authUserService.resetLoginErrorTimes(id))
        val user = authUserService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.loginErrorTimes == 0)
    }

    @Test
    fun incrementSecurityPasswordErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        val userBefore = authUserService.getUserRecord(id)
        val errorTimesBefore = userBefore?.securityPasswordErrorTimes ?: 0
        
        assertTrue(authUserService.incrementSecurityPasswordErrorTimes(id))
        val userAfter = authUserService.getUserRecord(id)
        assertNotNull(userAfter)
        assertTrue((userAfter.securityPasswordErrorTimes ?: 0) == errorTimesBefore + 1)
    }

    @Test
    fun resetSecurityPasswordErrorTimes() {
        val id = "a970f8c0-0000-0000-0000-000000000017"
        // 先增加错误次数
        authUserService.incrementSecurityPasswordErrorTimes(id)
        
        // 然后重置
        assertTrue(authUserService.resetSecurityPasswordErrorTimes(id))
        val user = authUserService.getUserRecord(id)
        assertNotNull(user)
        assertTrue(user.securityPasswordErrorTimes == 0)
    }
}
