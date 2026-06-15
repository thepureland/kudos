package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun batchBind_emptyUserIds_returnsZero() {
        assertEquals(0, authRoleUserService.batchBind("7817d37f-0000-0000-0000-000000000044", emptyList()))
    }

    @Test
    fun batchBind_nonExistentRole_rejected() {
        // Binding to a non-existent role must throw rather than silently insert orphan rows.
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.batchBind("no-such-role-id", listOf("7817d37f-0000-0000-0000-000000000040"))
        }
        assertTrue(ex.message?.contains("Role not found") == true, "message should name the missing role: ${ex.message}")
    }

    @Test
    fun batchBind_allAlreadyBound_returnsZero() {
        // role 043 already binds users 040 & 041; rebinding them adds nothing.
        val roleId = "7817d37f-0000-0000-0000-000000000043"
        val already = listOf("7817d37f-0000-0000-0000-000000000040", "7817d37f-0000-0000-0000-000000000041")
        assertEquals(0, authRoleUserService.batchBind(roleId, already))
    }

    @Test
    fun unbind_nonExistentRelation_returnsFalse() {
        assertFalse(authRoleUserService.unbind("7817d37f-0000-0000-0000-000000000043", "no-such-user"))
    }

    @Test
    fun batchBind_sodViolation_candidateIsRoleBSide_rejected() {
        // SoD pair a1 < a2 (canonical roleAId=a1, roleBId=a2); user b1 permanently holds the smaller
        // side a1. Binding the LARGER side a2 — i.e. the candidate equals the pair's roleBId — must be
        // rejected. Regression guard: a Criteria-aliasing bug in AuthRoleExclusionDao.searchByRoleId-
        // AndTenant once dropped every pair whose roleBId == candidate, silently bypassing this check.
        val roleB = "7817d37f-0000-0000-0000-0000000000a2"
        val userB = "7817d37f-0000-0000-0000-0000000000b1"
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.batchBind(roleB, listOf(userB))
        }
        assertTrue(ex.message?.contains("SoD") == true, "expected an SoD rejection, was: ${ex.message}")
        assertFalse(authRoleUserService.getUserIdsByRoleId(roleB).contains(userB))
    }
}
