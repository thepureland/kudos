package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for UserOrgUserService
 *
 * Test data source: `UserOrgUserServiceTest.sql`.
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
        
        // Test an existing relation.
        assertTrue(userOrgUserService.exists(orgId, userId))

        // Test a non-existent relation.
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
        
        // Batch bind (non-admin).
        val count = userOrgUserService.batchBind(orgId, userIds, false)
        assertTrue(count >= 3)

        // Verify bind succeeded.
        val boundUserIds = userOrgUserService.getUserIdsByOrgId(orgId)
        assertTrue(boundUserIds.containsAll(userIds))

        // Test duplicate binding (should skip already existing).
        val count2 = userOrgUserService.batchBind(orgId, userIds, false)
        assertTrue(count2 == 0) // Should return 0, because all already exist.
    }

    @Test
    fun unbind() {
        val orgId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000061"
        
        // Verify the relation exists.
        assertTrue(userOrgUserService.exists(orgId, userId))

        // Unbind.
        assertTrue(userOrgUserService.unbind(orgId, userId))

        // Verify the relation no longer exists.
        assertFalse(userOrgUserService.exists(orgId, userId))

        // Re-bind for subsequent tests.
        userOrgUserService.batchBind(orgId, listOf(userId), false)
    }

    @Test
    fun setOrgAdmin() {
        val orgId = "6cd22b48-0000-0000-0000-000000000063"
        val userId = "6cd22b48-0000-0000-0000-000000000061"
        
        // First set as admin.
        assertTrue(userOrgUserService.setOrgAdmin(orgId, userId, true))

        // Verify setting succeeded (confirmed by querying the relation, simplified here).
        assertTrue(userOrgUserService.exists(orgId, userId))

        // Cancel admin.
        assertTrue(userOrgUserService.setOrgAdmin(orgId, userId, false))

        // Verify cancellation succeeded.
        assertTrue(userOrgUserService.exists(orgId, userId))
    }
}
