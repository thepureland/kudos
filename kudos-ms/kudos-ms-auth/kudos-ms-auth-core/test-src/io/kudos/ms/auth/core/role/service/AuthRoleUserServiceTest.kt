package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleUserService
 *
 * Test data source: `AuthRoleUserServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

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
        assertTrue(roleIds.isNotEmpty())
        assertTrue(roleIds.contains("7817d37f-0000-0000-0000-000000000043"))
    }

    @Test
    fun exists() {
        val roleId = "7817d37f-0000-0000-0000-000000000043"
        val userId = "7817d37f-0000-0000-0000-000000000040"
        
        // Test an existing relation
        assertTrue(authRoleUserService.exists(roleId, userId))

        // Test a non-existent relation
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
        
        // Batch bind
        val count = authRoleUserService.batchBind(roleId, userIds)
        assertTrue(count >= 3)

        // Verify the binding succeeded
        val boundUserIds = authRoleUserService.getUserIdsByRoleId(roleId)
        assertTrue(boundUserIds.containsAll(userIds))

        // Test duplicate binding (existing entries should be skipped)
        val count2 = authRoleUserService.batchBind(roleId, userIds)
        assertTrue(count2 == 0) // should return 0 since all already exist
    }

    @Test
    fun unbind() {
        val roleId = "7817d37f-0000-0000-0000-000000000043"
        val userId = "7817d37f-0000-0000-0000-000000000041"
        
        // Verify the relation exists
        assertTrue(authRoleUserService.exists(roleId, userId))

        // Unbind
        assertTrue(authRoleUserService.unbind(roleId, userId))

        // Verify the relation no longer exists
        assertFalse(authRoleUserService.exists(roleId, userId))

        // Rebind so subsequent tests can run
        authRoleUserService.batchBind(roleId, listOf(userId))
    }
}
