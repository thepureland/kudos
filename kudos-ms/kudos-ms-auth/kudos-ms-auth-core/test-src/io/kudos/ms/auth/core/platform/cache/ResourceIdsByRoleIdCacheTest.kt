package io.kudos.ms.auth.core.platform.cache

import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleIdCacheHandler
 *
 * Test data source: `ResourceIdsByRoleIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByRoleIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByRoleIdCache

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // Existing role ID, has multiple resources
        var roleId = "699180cb-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(roleId)
        val resourceIds2 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds1.isNotEmpty(), "Role ${roleId} should have a list of resource IDs")
        assertEquals(resourceIds1, resourceIds2, "Two calls should return the same result (cache verification)")
        // Verify resource IDs: role ROLE_ADMIN has resource-aaa and resource-bbb
        assertEquals(2, resourceIds1.size, "Role ${roleId} should have 2 resource IDs")
        assertTrue(resourceIds1.contains("resource-aaa-wXxAqLrp"), "Should contain resource-aaa; actual returned: ${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb-wXxAqLrp"), "Should contain resource-bbb; actual returned: ${resourceIds1}")

        // Existing role ID, has multiple resources
        roleId = "699180cb-2222-2222-2222-222222222222"
        val resourceIds3 = cacheHandler.getResourceIds(roleId)
        val resourceIds4 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds3.isNotEmpty(), "Role ${roleId} should have a list of resource IDs")
        assertEquals(resourceIds3, resourceIds4, "Two calls should return the same result (cache verification)")
        // Role ROLE_USER has resource-ccc and resource-ddd
        assertEquals(
            2,
            resourceIds3.size,
            "Role ${roleId} should have 2 resource IDs; actual returned: ${resourceIds3}"
        )
        assertTrue(resourceIds3.contains("resource-ccc-wXxAqLrp"), "Should contain resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd-wXxAqLrp"), "Should contain resource-ddd")

        // Existing role ID, but no resources (clear any existing cache first)
        roleId = "699180cb-3333-3333-3333-333333333333"
        cacheHandler.evict(roleId) // Clear any existing cache
        val resourceIds5 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds5.isEmpty(), "Role ${roleId} has no resources, should return empty list; actual returned: ${resourceIds5}")

        // Non-existent role ID
        roleId = "no_exist_role_id"
        val resourceIds6 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds6.isEmpty(), "Non-existent role ID should return empty list")
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "699180cb-3333-3333-3333-333333333333"
        val resourceId = "resource-fff"

        // Clear any existing cache first to ensure a clean test environment
        cacheHandler.evict(roleId)

        // Fetch once to record the initial resource count
        val resourceIdsBefore = cacheHandler.getResourceIds(roleId)
        val beforeSize = resourceIdsBefore.size

        // Insert a new role-resource relation record
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)

        // Sync cache (simulating role-resource relation change)
        cacheHandler.syncOnRoleResourceChange(roleId)

        // Verify cache has been cleared and reloaded; should contain the newly inserted resource
        val resourceIdsAfter = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsAfter.size > beforeSize, "After sync, should contain the newly inserted resource ID; before: ${beforeSize}, after: ${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "Should contain the newly inserted resource ID")

        // Clean up test data
        authRoleResourceDao.deleteById(id)
        // Clean up cache to avoid affecting other tests
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnBatchRoleResourceChange() {
        val roleId1 = "699180cb-3333-3333-3333-333333333333"
        val roleId2 = "699180cb-3333-3333-3333-333333333333"
        val resourceId1 = "resource-ggg"
        val resourceId2 = "resource-hhh"
        val roleIds = listOf(roleId1, roleId2)

        // Fetch once to record the initial resource count
        val resourceIds1Before = cacheHandler.getResourceIds(roleId1)
        val beforeSize = resourceIds1Before.size

        // Batch insert role-resource relation records
        val authRoleResource1 = AuthRoleResource.Companion().apply {
            this.roleId = roleId1
            this.resourceId = resourceId1
        }
        val id1 = authRoleResourceDao.insert(authRoleResource1)

        val authRoleResource2 = AuthRoleResource.Companion().apply {
            this.roleId = roleId2
            this.resourceId = resourceId2
        }
        val id2 = authRoleResourceDao.insert(authRoleResource2)

        // Batch sync cache (simulating batch role-resource relation changes)
        cacheHandler.syncOnBatchRoleResourceChange(roleIds)

        // Verify cache has been cleared and reloaded; should contain newly inserted resources
        val resourceIds1After = cacheHandler.getResourceIds(roleId1)
        assertTrue(resourceIds1After.size > beforeSize, "After sync, should contain the newly inserted resource ID")

        // Clean up test data
        authRoleResourceDao.deleteById(id1)
        authRoleResourceDao.deleteById(id2)
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "699180cb-3333-3333-3333-333333333333"
        val resourceId = "resource-iii"

        // First insert a role-resource relation record
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)

        // Sync cache first to ensure the newly inserted data is in the cache
        cacheHandler.syncOnRoleResourceChange(roleId)

        // Fetch once to ensure data is in the cache
        val resourceIdsBefore = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsBefore.contains(resourceId), "The newly inserted resource relation should be in the cache")

        // Delete the database record (simulating role deletion or role-resource relation deletion)
        val deleteSuccess = authRoleResourceDao.deleteById(id)
        assertTrue(deleteSuccess, "Deletion should succeed")

        // Directly drive the event listener (AFTER_COMMIT does not fire in @Transactional tests, so invoke on(...) directly)
        cacheHandler.on(AuthRoleDeleted(roleId, tenantId = "tenant-x", code = "code-x"))

        // Verify cache has been cleared; fetching again should not contain the deleted resource
        val resourceIdsAfter = cacheHandler.getResourceIds(roleId)
        assertTrue(!resourceIdsAfter.contains(resourceId), "After deletion, the cache should be cleared and should not contain the deleted resource ID")
    }

}
