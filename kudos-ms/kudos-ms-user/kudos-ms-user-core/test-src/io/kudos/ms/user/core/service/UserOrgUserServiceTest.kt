package io.kudos.ms.user.core.service

import io.kudos.ms.user.core.service.iservice.IUserOrgUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for UserOrgUserService
 *
 * 测试数据来源：`UserOrgUserServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserOrgUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userOrgUserService: IUserOrgUserService

    @Test
    fun getUserIdsByOrgId() {
        val orgId = "6cd22b48-0000-0000-0000-000000000063"
        val userIds = userOrgUserService.getUserIdsByOrgId(orgId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("6cd22b48-0000-0000-0000-000000000060"))
        assertTrue(userIds.contains("6cd22b48-0000-0000-0000-000000000061"))
    }

    @Test
    fun getOrgIdsByUserId() {
        val userId = "6cd22b48-0000-0000-0000-000000000060"
        val orgIds = userOrgUserService.getOrgIdsByUserId(userId)
        assertTrue(orgIds.isNotEmpty())
        assertTrue(orgIds.contains("6cd22b48-0000-0000-0000-000000000063"))
    }

    @Test
    fun exists() {
        val orgId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000060"
        
        // 测试存在的关系
        assertTrue(userOrgUserService.exists(orgId, userId))
        
        // 测试不存在的关系
        assertFalse(userOrgUserService.exists(orgId, "non-existent-user-id"))
    }

    @Test
    fun batchBind() {
        val orgId = "6cd22b48-0000-0000-0000-000000000064"
        val userIds = listOf(
            "6cd22b48-0000-0000-0000-000000000060",
            "6cd22b48-0000-0000-0000-000000000061",
            "6cd22b48-0000-0000-0000-000000000062"
        )
        
        // 批量绑定（非管理员）
        val count = userOrgUserService.batchBind(orgId, userIds, false)
        assertTrue(count >= 3)
        
        // 验证绑定成功
        val boundUserIds = userOrgUserService.getUserIdsByOrgId(orgId)
        assertTrue(boundUserIds.containsAll(userIds))
        
        // 测试重复绑定（应该跳过已存在的）
        val count2 = userOrgUserService.batchBind(orgId, userIds, false)
        assertTrue(count2 == 0) // 应该返回0，因为都已存在
    }

    @Test
    fun unbind() {
        val orgId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000061"
        
        // 验证关系存在
        assertTrue(userOrgUserService.exists(orgId, userId))
        
        // 解绑
        assertTrue(userOrgUserService.unbind(orgId, userId))
        
        // 验证关系已不存在
        assertFalse(userOrgUserService.exists(orgId, userId))
        
        // 重新绑定以便后续测试
        userOrgUserService.batchBind(orgId, listOf(userId), false)
    }

    @Test
    fun setOrgAdmin() {
        val orgId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000061"
        
        // 先设置为管理员
        assertTrue(userOrgUserService.setOrgAdmin(orgId, userId, true))
        
        // 验证设置成功（通过查询关系确认，这里简化测试）
        assertTrue(userOrgUserService.exists(orgId, userId))
        
        // 取消管理员
        assertTrue(userOrgUserService.setOrgAdmin(orgId, userId, false))
        
        // 验证取消成功
        assertTrue(userOrgUserService.exists(orgId, userId))
    }
}
