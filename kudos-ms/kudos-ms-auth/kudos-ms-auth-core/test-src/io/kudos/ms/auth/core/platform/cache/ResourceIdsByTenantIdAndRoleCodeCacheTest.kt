package io.kudos.ms.auth.core.platform.cache

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleCodeCacheHandler
 *
 * Test data source: `ResourceIdsByTenantIdAndRoleCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenantIdAndRoleCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenantIdAndRoleCodeCache

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // Existing tenant and role
        var tenantId = "tenant-001-174d0234"
        var roleCode = "ROLE_ADMIN"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, roleCode)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // Non-existent role
        roleCode = "ROLE_NO_EXIST"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds4.isEmpty())

        // Non-existent tenant
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds5.isEmpty())
    }

    @Test
    fun syncOnRoleResourceInsert() {
        val tenantId = "tenant-001-174d0234"
        val roleCode = "ROLE_USER"
        val roleId = "174d0234-2222-2222-2222-222222222222" // ID of ROLE_USER
        val resourceId = "resource-xxx" // New resource ID (use a non-existing resource ID to avoid unique constraint conflicts)

        // Clear any existing cache first to ensure a clean test environment
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        // Fetch once to record the initial resource count
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        val beforeSize = resourceIdsBefore.size

        // Check whether the relation already exists; if so, delete it first (in case previous tests did not clean up)
        if (authRoleResourceDao.exists(roleId, resourceId)) {
            val criteria = Criteria.of(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
                .addAnd(AuthRoleResource::resourceId.name, OperatorEnum.EQ, resourceId)
            authRoleResourceDao.batchDeleteCriteria(criteria)
        }

        // Insert a new role-resource relation record
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)

        // Sync cache (simulating role-resource relation insertion)
        cacheHandler.syncOnRoleResourceInsert(tenantId, roleCode)

        // Verify cache has been cleared and reloaded; should contain the newly inserted resource
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.size > beforeSize, "After sync, should contain the newly inserted resource ID; before: ${beforeSize}, after: ${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "Should contain the newly inserted resource ID: ${resourceId}; actual returned: ${resourceIdsAfter}")

        // Clean up test data
        authRoleResourceDao.deleteById(id)
        // Clean up cache to avoid affecting other tests
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
    }

    @Test
    fun syncOnRoleResourceDelete() {
        val tenantId = "tenant-001-174d0234"
        val roleCode = "ROLE_USER"
        val roleId = "174d0234-2222-2222-2222-222222222222" // ID of ROLE_USER
        val resourceId = "resource-eee" // New resource ID

        // First insert a role-resource relation record
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)

        // Sync cache first to ensure the newly inserted data is in the cache
        cacheHandler.syncOnRoleResourceInsert(tenantId, roleCode)

        // Fetch once to ensure data is in the cache
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsBefore.contains(resourceId), "The newly inserted resource relation should be in the cache")

        // Delete the database record
        val deleteSuccess = authRoleResourceDao.deleteById(id)
        assertTrue(deleteSuccess, "Deletion should succeed")

        // Sync cache (simulating role-resource relation deletion)
        cacheHandler.syncOnRoleResourceDelete(tenantId, roleCode)

        // Verify cache has been cleared and reloaded; should not contain the deleted resource
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(!resourceIdsAfter.contains(resourceId), "After deletion, the cache should be cleared and should not contain the deleted resource ID")
    }

    @Test
    fun syncOnRoleUpdate() {
        val oldTenantId = "tenant-001-174d0234"
        val oldRoleCode = "ROLE_USER"
        val newTenantId = "tenant-001-174d0234"
        val newRoleCode = "ROLE_USER_UPDATED"
        val roleId = "174d0234-2222-2222-2222-222222222222" // ID of ROLE_USER

        // Fetch once to ensure data is in the cache
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldRoleCode)

        // Update role code
        val role = authRoleDao.get(roleId)
        assertTrue(role != null, "Role should exist")
        val success = authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to newRoleCode))
        assertTrue(success, "Update should succeed")

        // Sync cache (simulating role info update)
        cacheHandler.syncOnRoleUpdate(oldTenantId, oldRoleCode, newTenantId, newRoleCode)

        // Verify the old cache has been cleared and the new cache can fetch data
//        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldRoleCode)
        val resourceIdsNew = cacheHandler.getResourceIds(newTenantId, newRoleCode)
        // The old cache should be cleared and the new cache should fetch data (resource relations unchanged, only role code changed)
        assertEquals(
            resourceIdsBefore.size,
            resourceIdsNew.size,
            "The new role code should fetch the same resource list"
        )

        // Restore role code
        authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to oldRoleCode))
    }

    @Test
    fun syncOnRoleDelete() {
        val tenantId = "tenant-001-174d0234"
        val roleCode = "ROLE_USER"

        // Fetch once to ensure data is in the cache (even if empty list)
        cacheHandler.getResourceIds(tenantId, roleCode)

        // Delete the role record from the database
        val roleId = "174d0234-2222-2222-2222-222222222222" // ID of ROLE_USER
        authRoleDao.deleteById(roleId)

        // Directly drive both listeners (AFTER_COMMIT does not fire in @Transactional tests):
        // In production, the AuthRoleDeleted event triggers both this cache and AuthRoleHashCache's on(...).
        val event = AuthRoleDeleted(roleId, tenantId, roleCode)
        cacheHandler.on(event)
        authRoleHashCache.on(event)

        // Verify cache has been cleared; fetching again should return an empty list (since the role no longer exists)
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.isEmpty(), "After deleting the role, the cache should be cleared and fetching again should return an empty list")
    }

}
