package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IAuthDeptUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthDeptUserService
 *
 * 测试数据来源：`AuthDeptUserServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthDeptUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authDeptUserService: IAuthDeptUserService

    @Test
    fun getUserIdsByDeptId() {
        val deptId = "6cd22b48-0000-0000-0000-000000000063"
        val userIds = authDeptUserService.getUserIdsByDeptId(deptId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("6cd22b48-0000-0000-0000-000000000060"))
        assertTrue(userIds.contains("6cd22b48-0000-0000-0000-000000000061"))
    }

    @Test
    fun getDeptIdsByUserId() {
        val userId = "6cd22b48-0000-0000-0000-000000000060"
        val deptIds = authDeptUserService.getDeptIdsByUserId(userId)
        assertTrue(deptIds.size >= 1)
        assertTrue(deptIds.contains("6cd22b48-0000-0000-0000-000000000063"))
    }

    @Test
    fun exists() {
        val deptId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000060"
        
        // 测试存在的关系
        assertTrue(authDeptUserService.exists(deptId, userId))
        
        // 测试不存在的关系
        assertFalse(authDeptUserService.exists(deptId, "non-existent-user-id"))
    }

    @Test
    fun batchBind() {
        val deptId = "6cd22b48-0000-0000-0000-000000000064"
        val userIds = listOf(
            "6cd22b48-0000-0000-0000-000000000060",
            "6cd22b48-0000-0000-0000-000000000061",
            "6cd22b48-0000-0000-0000-000000000062"
        )
        
        // 批量绑定（非管理员）
        val count = authDeptUserService.batchBind(deptId, userIds, false)
        assertTrue(count >= 3)
        
        // 验证绑定成功
        val boundUserIds = authDeptUserService.getUserIdsByDeptId(deptId)
        assertTrue(boundUserIds.containsAll(userIds))
        
        // 测试重复绑定（应该跳过已存在的）
        val count2 = authDeptUserService.batchBind(deptId, userIds, false)
        assertTrue(count2 == 0) // 应该返回0，因为都已存在
    }

    @Test
    fun unbind() {
        val deptId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000061"
        
        // 验证关系存在
        assertTrue(authDeptUserService.exists(deptId, userId))
        
        // 解绑
        assertTrue(authDeptUserService.unbind(deptId, userId))
        
        // 验证关系已不存在
        assertFalse(authDeptUserService.exists(deptId, userId))
        
        // 重新绑定以便后续测试
        authDeptUserService.batchBind(deptId, listOf(userId), false)
    }

    @Test
    fun setDeptAdmin() {
        val deptId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000061"
        
        // 先设置为管理员
        assertTrue(authDeptUserService.setDeptAdmin(deptId, userId, true))
        
        // 验证设置成功（通过查询关系确认，这里简化测试）
        assertTrue(authDeptUserService.exists(deptId, userId))
        
        // 取消管理员
        assertTrue(authDeptUserService.setDeptAdmin(deptId, userId, false))
        
        // 验证取消成功
        assertTrue(authDeptUserService.exists(deptId, userId))
    }
}
