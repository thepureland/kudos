package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IUserLogLoginService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * junit test for UserLogLoginService
 *
 * 测试数据来源：`UserLogLoginServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserLogLoginServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userLogLoginService: IUserLogLoginService

    @Test
    fun getLoginsByUserId() {
        val userId = "1a14e2ae-0000-0000-0000-000000000078"
        val logins = userLogLoginService.getLoginsByUserId(userId, 100)
        assertTrue(logins.size >= 4)
        assertTrue(logins.any { it.username == "svc-user-loglog-test-1-88Sexq53" })

        // 验证按时间倒序
        if (logins.size > 1) {
            assertTrue(logins[0].loginTime.isAfter(logins[1].loginTime) || logins[0].loginTime.isEqual(logins[1].loginTime))
        }
    }

    @Test
    fun getLoginsByTenantId() {
        val tenantId = "svc-tenan-loglo-test-1-88Sexq53"
        val logins = userLogLoginService.getLoginsByTenantId(tenantId, 100)
        assertTrue(logins.size >= 6)

        // 验证按时间倒序
        if (logins.size > 1) {
            assertTrue(logins[0].loginTime.isAfter(logins[1].loginTime) || logins[0].loginTime.isEqual(logins[1].loginTime))
        }
    }

    @Test
    fun getLoginsByTimeRange() {
        val tenantId = "svc-tenan-loglo-test-1-88Sexq53"
        val startTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 5, 23, 59, 59)

        val logins = userLogLoginService.getLoginsByTimeRange(tenantId, null, startTime, endTime)
        assertTrue(logins.size >= 5)

        // 验证时间范围
        logins.forEach {
            assertTrue(it.loginTime.isAfter(startTime) || it.loginTime.isEqual(startTime))
            assertTrue(it.loginTime.isBefore(endTime) || it.loginTime.isEqual(endTime))
        }

        // 测试指定用户
        val userId = "1a14e2ae-0000-0000-0000-000000000078"
        val userLogins = userLogLoginService.getLoginsByTimeRange(tenantId, userId, startTime, endTime)
        assertTrue(userLogins.size >= 3)
        assertTrue(userLogins.all { it.userId == userId })
    }

    @Test
    fun getRecentLogins() {
        val tenantId = "svc-tenan-loglo-test-1-88Sexq53"
        val logins = userLogLoginService.getRecentLogins(tenantId, null, 5)
        assertTrue(logins.size <= 5)

        // 验证按时间倒序
        if (logins.size > 1) {
            assertTrue(logins[0].loginTime.isAfter(logins[1].loginTime) || logins[0].loginTime.isEqual(logins[1].loginTime))
        }

        // 测试指定用户
        val userId = "1a14e2ae-0000-0000-0000-000000000078"
        val userLogins = userLogLoginService.getRecentLogins(tenantId, userId, 3)
        assertTrue(userLogins.size <= 3)
        assertTrue(userLogins.all { it.userId == userId })
    }

    @Test
    fun countLogins() {
        val tenantId = "svc-tenan-loglo-test-1-88Sexq53"
        val count = userLogLoginService.countLogins(tenantId, null, null, null)
        assertTrue(count >= 6)

        // 测试指定用户
        val userId = "1a14e2ae-0000-0000-0000-000000000078"
        val userCount = userLogLoginService.countLogins(tenantId, userId, null, null)
        assertTrue(userCount >= 4)

        // 测试时间范围
        val startTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 5, 23, 59, 59)
        val rangeCount = userLogLoginService.countLogins(tenantId, null, startTime, endTime)
        assertTrue(rangeCount >= 5)
    }

    @Test
    fun countSuccessLogins() {
        val tenantId = "svc-tenan-loglo-test-1-88Sexq53"
        val count = userLogLoginService.countSuccessLogins(tenantId, null, null, null)
        assertTrue(count >= 4) // 至少有4条成功记录

        // 测试指定用户
        val userId = "1a14e2ae-0000-0000-0000-000000000078"
        val userCount = userLogLoginService.countSuccessLogins(tenantId, userId, null, null)
        assertTrue(userCount >= 3) // 用户1至少有3条成功记录
    }

    @Test
    fun countFailureLogins() {
        val tenantId = "svc-tenan-loglo-test-1-88Sexq53"
        val count = userLogLoginService.countFailureLogins(tenantId, null, null, null)
        assertTrue(count >= 2) // 至少有2条失败记录

        // 测试指定用户
        val userId = "1a14e2ae-0000-0000-0000-000000000078"
        val userCount = userLogLoginService.countFailureLogins(tenantId, userId, null, null)
        assertTrue(userCount >= 1) // 用户1至少有1条失败记录
    }
}