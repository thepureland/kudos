package io.kudos.ams.auth.core.service

import io.kudos.ams.auth.core.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleUserService
 *
 * 测试数据来源：`AuthRoleUserServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleUserService: io.kudos.ams.auth.core.service.iservice.IAuthRoleUserService

    @Test
    fun getUserIdsByRoleId() {
        val roleId = "7817d37f-0000-0000-0000-000000000043"
        val userIds = authRoleUserService.getUserIdsByRoleId(roleId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("7817d37f-0000-0000-0000-000000000040"))
        assertTrue(userIds.contains("7817d37f-0000-0000-0000-000000000041"))
    }

    @Test
    fun getRoleIdsByUserId() {
        val userId = "7817d37f-0000-0000-0000-000000000040"
        val roleIds = authRoleUserService.getRoleIdsByUserId(userId)
        assertTrue(roleIds.size >= 1)
        assertTrue(roleIds.contains("7817d37f-0000-0000-0000-000000000043"))
    }

    @Test
    fun exists() {
        val roleId = "7817d37f-0000-0000-0000-000000000043"
        val userId = "7817d37f-0000-0000-0000-000000000040"
        
        // 测试存在的关系
        assertTrue(authRoleUserService.exists(roleId, userId))
        
        // 测试不存在的关系
        assertFalse(authRoleUserService.exists(roleId, "non-existent-user-id"))
    }

    @Test
    fun batchBind() {
        val roleId = "7817d37f-0000-0000-0000-000000000044"
        val userIds = listOf(
            "7817d37f-0000-0000-0000-000000000040",
            "7817d37f-0000-0000-0000-000000000041",
            "7817d37f-0000-0000-0000-000000000042"
        )
        
        // 批量绑定
        val count = authRoleUserService.batchBind(roleId, userIds)
        assertTrue(count >= 3)
        
        // 验证绑定成功
        val boundUserIds = authRoleUserService.getUserIdsByRoleId(roleId)
        assertTrue(boundUserIds.containsAll(userIds))
        
        // 测试重复绑定（应该跳过已存在的）
        val count2 = authRoleUserService.batchBind(roleId, userIds)
        assertTrue(count2 == 0) // 应该返回0，因为都已存在
    }

    @Test
    fun unbind() {
        val roleId = "7817d37f-0000-0000-0000-000000000043"
        val userId = "7817d37f-0000-0000-0000-000000000041"
        
        // 验证关系存在
        assertTrue(authRoleUserService.exists(roleId, userId))
        
        // 解绑
        assertTrue(authRoleUserService.unbind(roleId, userId))
        
        // 验证关系已不存在
        assertFalse(authRoleUserService.exists(roleId, userId))
        
        // 重新绑定以便后续测试
        authRoleUserService.batchBind(roleId, listOf(userId))
    }
}
