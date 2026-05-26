package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthGroupUserService
 *
 * Test data source: `AuthGroupUserServiceTest.sql`
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
        assertTrue(groupIds.isNotEmpty())
        assertTrue(groupIds.contains("9c1b2a3d-0000-0000-0000-000000000083"))
    }

    @Test
    fun exists() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000080"

        // Test existing relation
        assertTrue(authGroupUserService.exists(groupId, userId))

        // Test non-existing relation
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

        // Batch bind
        val count = authGroupUserService.batchBind(groupId, userIds)
        assertTrue(count >= 3)

        // Verify binding succeeded
        val boundUserIds = authGroupUserService.getUserIdsByGroupId(groupId)
        assertTrue(boundUserIds.containsAll(userIds))

        // Test duplicate binding (should skip existing ones)
        val count2 = authGroupUserService.batchBind(groupId, userIds)
        assertTrue(count2 == 0) // Should return 0 because all already exist
    }

    @Test
    fun unbind() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000081"

        // Verify relation exists
        assertTrue(authGroupUserService.exists(groupId, userId))

        // Unbind
        assertTrue(authGroupUserService.unbind(groupId, userId))

        // Verify relation no longer exists
        assertFalse(authGroupUserService.exists(groupId, userId))

        // Re-bind for subsequent tests
        authGroupUserService.batchBind(groupId, listOf(userId))
    }
}
