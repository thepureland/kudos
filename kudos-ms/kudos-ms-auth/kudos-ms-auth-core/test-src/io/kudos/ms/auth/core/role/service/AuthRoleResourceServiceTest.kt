package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleResourceService
 *
 * Test data source: `AuthRoleResourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleResourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleResourceService: IAuthRoleResourceService

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
        assertTrue(roleIds.isNotEmpty())
        assertTrue(roleIds.contains("3248fb0d-0000-0000-0000-000000000050"))
    }

    @Test
    fun exists() {
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        val resourceId = "3248fb0d-0000-0000-0000-000000000056"
        
        // Test an existing relation
        assertTrue(authRoleResourceService.exists(roleId, resourceId))

        // Test a non-existent relation
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
        
        // Batch bind
        val count = authRoleResourceService.batchBind(roleId, resourceIds)
        assertTrue(count >= 3)

        // Verify the binding succeeded
        val boundResourceIds = authRoleResourceService.getResourceIdsByRoleId(roleId)
        assertTrue(boundResourceIds.containsAll(resourceIds))

        // Test duplicate binding (existing entries should be skipped)
        val count2 = authRoleResourceService.batchBind(roleId, resourceIds)
        assertEquals(count2, 0) // should return 0 since all already exist
    }

    @Test
    fun unbind() {
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        val resourceId = "3248fb0d-0000-0000-0000-000000000057"

        // Verify the relation exists
        assertTrue(authRoleResourceService.exists(roleId, resourceId))

        // Unbind
        assertTrue(authRoleResourceService.unbind(roleId, resourceId))

        // Verify the relation no longer exists
        assertFalse(authRoleResourceService.exists(roleId, resourceId))

        // Rebind so subsequent tests can run
        authRoleResourceService.batchBind(roleId, listOf(resourceId))
    }

    @Test
    fun batchBind_emptyResourceIds_returnsZero() {
        assertEquals(0, authRoleResourceService.batchBind("3248fb0d-0000-0000-0000-000000000051", emptyList()))
    }

    @Test
    fun batchBind_allAlreadyBound_returnsZero() {
        // role 050 already grants 056 & 057; rebinding adds nothing.
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        val already = listOf("3248fb0d-0000-0000-0000-000000000056", "3248fb0d-0000-0000-0000-000000000057")
        assertEquals(0, authRoleResourceService.batchBind(roleId, already))
    }

    @Test
    fun unbind_nonExistentRelation_returnsFalse() {
        assertFalse(authRoleResourceService.unbind("3248fb0d-0000-0000-0000-000000000050", "no-such-resource"))
    }

    @Test
    fun exists_trimsResourceId() {
        // exists() trims its argument, so trailing whitespace still matches.
        val roleId = "3248fb0d-0000-0000-0000-000000000050"
        assertTrue(authRoleResourceService.exists(roleId, "3248fb0d-0000-0000-0000-000000000056  "))
    }
}
