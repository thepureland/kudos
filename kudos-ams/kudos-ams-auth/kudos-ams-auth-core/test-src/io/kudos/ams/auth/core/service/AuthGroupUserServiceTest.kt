package io.kudos.ams.auth.core.service

import io.kudos.ams.auth.core.service.iservice.IAuthGroupUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthGroupUserService
 *
 * 测试数据来源：`AuthGroupUserServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authGroupUserService: IAuthGroupUserService

    @Test
    fun getUserIdsByGroupId() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userIds = authGroupUserService.getUserIdsByGroupId(groupId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("9c1b2a3d-0000-0000-0000-000000000080"))
        assertTrue(userIds.contains("9c1b2a3d-0000-0000-0000-000000000081"))
    }

    @Test
    fun getGroupIdsByUserId() {
        val userId = "9c1b2a3d-0000-0000-0000-000000000080"
        val groupIds = authGroupUserService.getGroupIdsByUserId(userId)
        assertTrue(groupIds.size >= 1)
        assertTrue(groupIds.contains("9c1b2a3d-0000-0000-0000-000000000083"))
    }

    @Test
    fun exists() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000080"

        // 测试存在的关系
        assertTrue(authGroupUserService.exists(groupId, userId))

        // 测试不存在的关系
        assertFalse(authGroupUserService.exists(groupId, "non-existent-user-id"))
    }

    @Test
    fun batchBind() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000084"
        val userIds = listOf(
            "9c1b2a3d-0000-0000-0000-000000000080",
            "9c1b2a3d-0000-0000-0000-000000000081",
            "9c1b2a3d-0000-0000-0000-000000000082"
        )

        // 批量绑定
        val count = authGroupUserService.batchBind(groupId, userIds)
        assertTrue(count >= 3)

        // 验证绑定成功
        val boundUserIds = authGroupUserService.getUserIdsByGroupId(groupId)
        assertTrue(boundUserIds.containsAll(userIds))

        // 测试重复绑定（应该跳过已存在的）
        val count2 = authGroupUserService.batchBind(groupId, userIds)
        assertTrue(count2 == 0) // 应该返回0，因为都已存在
    }

    @Test
    fun unbind() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000081"

        // 验证关系存在
        assertTrue(authGroupUserService.exists(groupId, userId))

        // 解绑
        assertTrue(authGroupUserService.unbind(groupId, userId))

        // 验证关系已不存在
        assertFalse(authGroupUserService.exists(groupId, userId))

        // 重新绑定以便后续测试
        authGroupUserService.batchBind(groupId, listOf(userId))
    }
}
