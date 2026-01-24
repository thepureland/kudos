package io.kudos.ams.auth.provider.service

import io.kudos.ams.auth.provider.service.iservice.IAuthLogLoginService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertTrue
import java.time.LocalDateTime

/**
 * junit test for AuthLogLoginService
 *
 * 测试数据来源：`AuthLogLoginServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthLogLoginServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authLogLoginService: IAuthLogLoginService

    @Test
    fun getLoginsByUserId() {
        val userId = "30000000-0000-0000-0000-000000000078"
        val logins = authLogLoginService.getLoginsByUserId(userId, 100)
        assertTrue(logins.size >= 4)
        assertTrue(logins.any { it.username == "svc-user-loglogin-test-1" })
        
        // 验证按时间倒序
        if (logins.size > 1) {
            assertTrue(logins[0].loginTime.isAfter(logins[1].loginTime) || logins[0].loginTime.isEqual(logins[1].loginTime))
        }
    }

    @Test
    fun getLoginsByTenantId() {
        val tenantId = "svc-tenant-loglogin-test-1"
        val logins = authLogLoginService.getLoginsByTenantId(tenantId, 100)
        assertTrue(logins.size >= 6)
        
        // 验证按时间倒序
        if (logins.size > 1) {
            assertTrue(logins[0].loginTime.isAfter(logins[1].loginTime) || logins[0].loginTime.isEqual(logins[1].loginTime))
        }
    }

    @Test
    fun getLoginsByTimeRange() {
        val tenantId = "svc-tenant-loglogin-test-1"
        val startTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 5, 23, 59, 59)
        
        val logins = authLogLoginService.getLoginsByTimeRange(tenantId, null, startTime, endTime)
        assertTrue(logins.size >= 5)
        
        // 验证时间范围
        logins.forEach {
            assertTrue(it.loginTime.isAfter(startTime) || it.loginTime.isEqual(startTime))
            assertTrue(it.loginTime.isBefore(endTime) || it.loginTime.isEqual(endTime))
        }
        
        // 测试指定用户
        val userId = "30000000-0000-0000-0000-000000000078"
        val userLogins = authLogLoginService.getLoginsByTimeRange(tenantId, userId, startTime, endTime)
        assertTrue(userLogins.size >= 3)
        assertTrue(userLogins.all { it.userId == userId })
    }

    @Test
    fun getRecentLogins() {
        val tenantId = "svc-tenant-loglogin-test-1"
        val logins = authLogLoginService.getRecentLogins(tenantId, null, 5)
        assertTrue(logins.size <= 5)
        
        // 验证按时间倒序
        if (logins.size > 1) {
            assertTrue(logins[0].loginTime.isAfter(logins[1].loginTime) || logins[0].loginTime.isEqual(logins[1].loginTime))
        }
        
        // 测试指定用户
        val userId = "30000000-0000-0000-0000-000000000078"
        val userLogins = authLogLoginService.getRecentLogins(tenantId, userId, 3)
        assertTrue(userLogins.size <= 3)
        assertTrue(userLogins.all { it.userId == userId })
    }

    @Test
    fun countLogins() {
        val tenantId = "svc-tenant-loglogin-test-1"
        val count = authLogLoginService.countLogins(tenantId, null, null, null)
        assertTrue(count >= 6)
        
        // 测试指定用户
        val userId = "30000000-0000-0000-0000-000000000078"
        val userCount = authLogLoginService.countLogins(tenantId, userId, null, null)
        assertTrue(userCount >= 4)
        
        // 测试时间范围
        val startTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 5, 23, 59, 59)
        val rangeCount = authLogLoginService.countLogins(tenantId, null, startTime, endTime)
        assertTrue(rangeCount >= 5)
    }

    @Test
    fun countSuccessLogins() {
        val tenantId = "svc-tenant-loglogin-test-1"
        val count = authLogLoginService.countSuccessLogins(tenantId, null, null, null)
        assertTrue(count >= 4) // 至少有4条成功记录
        
        // 测试指定用户
        val userId = "30000000-0000-0000-0000-000000000078"
        val userCount = authLogLoginService.countSuccessLogins(tenantId, userId, null, null)
        assertTrue(userCount >= 3) // 用户1至少有3条成功记录
    }

    @Test
    fun countFailureLogins() {
        val tenantId = "svc-tenant-loglogin-test-1"
        val count = authLogLoginService.countFailureLogins(tenantId, null, null, null)
        assertTrue(count >= 2) // 至少有2条失败记录
        
        // 测试指定用户
        val userId = "30000000-0000-0000-0000-000000000078"
        val userCount = authLogLoginService.countFailureLogins(tenantId, userId, null, null)
        assertTrue(userCount >= 1) // 用户1至少有1条失败记录
    }
}
