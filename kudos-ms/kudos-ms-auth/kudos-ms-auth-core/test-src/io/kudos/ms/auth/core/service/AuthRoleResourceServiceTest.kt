package io.kudos.ms.auth.core.service

import io.kudos.ms.auth.core.service.iservice.IAuthRoleResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleResourceService
 *
 * 测试数据来源：`AuthRoleResourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleResourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleResourceService: io.kudos.ms.auth.core.service.iservice.IAuthRoleResourceService

    @Test
    fun getResourceIdsByRoleId() {
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        val resourceIds = authRoleResourceService.getResourceIdsByRoleId(roleId)
        assertTrue(resourceIds.size >= 2)
        assertTrue(resourceIds.contains("3248fb0d-0000-0000-0000-000000000056"))
        assertTrue(resourceIds.contains("3248fb0d-0000-0000-0000-000000000057"))
    }

    @Test
    fun getRoleIdsByResourceId() {
        val resourceId = "3248fb0d-0000-0000-0000-000000000056"
        val roleIds = authRoleResourceService.getRoleIdsByResourceId(resourceId)
        assertTrue(roleIds.size >= 1)
        assertTrue(roleIds.contains("3248fb0d-0000-0000-0000-000000000050"))
    }

    @Test
    fun exists() {
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        val resourceId = "3248fb0d-0000-0000-0000-000000000056"
        
        // 测试存在的关系
        assertTrue(authRoleResourceService.exists(roleId, resourceId))
        
        // 测试不存在的关系
        assertFalse(authRoleResourceService.exists(roleId, "non-existent-resource-id"))
    }

    @Test
    fun batchBind() {
        val roleId = "3248fb0d-0000-0000-0000-000000000051"
        val resourceIds = listOf(
            "3248fb0d-0000-0000-0000-000000000056",
            "3248fb0d-0000-0000-0000-000000000057",
            "30000000-0000-0000-0000-000000000058"
        )
        
        // 批量绑定
        val count = authRoleResourceService.batchBind(roleId, resourceIds)
        assertTrue(count >= 3)
        
        // 验证绑定成功
        val boundResourceIds = authRoleResourceService.getResourceIdsByRoleId(roleId)
        assertTrue(boundResourceIds.containsAll(resourceIds))
        
        // 测试重复绑定（应该跳过已存在的）
        val count2 = authRoleResourceService.batchBind(roleId, resourceIds)
        assertTrue(count2 == 0) // 应该返回0，因为都已存在
    }

    @Test
    fun unbind() {
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        val resourceId = "3248fb0d-0000-0000-0000-000000000057"
        
        // 验证关系存在
        assertTrue(authRoleResourceService.exists(roleId, resourceId))
        
        // 解绑
        assertTrue(authRoleResourceService.unbind(roleId, resourceId))
        
        // 验证关系已不存在
        assertFalse(authRoleResourceService.exists(roleId, resourceId))
        
        // 重新绑定以便后续测试
        authRoleResourceService.batchBind(roleId, listOf(resourceId))
    }
}
