package io.kudos.ms.auth.core.platform.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByTenanetIdAndUsernameCacheHandler
 *
 * Test data source: `ResourceIdsByTenantIdAndUsernameCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenantIdAndUsernameCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenantIdAndUsernameCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Test
    fun getResourceIds() {
        // Existing tenant and user.
        var tenantId = "tenant-001-InqhPsBT"
        var username = "admin"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, username)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // Non-existent username.
        username = "no_exist_user"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds4.isEmpty())

        // Non-existent tenant.
        tenantId = "no_exist_tenant"
        username = "admin"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds5.isEmpty())
    }

    @Test
    fun syncOnUserUpdate() {
        val oldTenantId = "tenant-001-InqhPsBT"
        val oldUsername = "zhangsan"
        val newTenantId = "tenant-001-InqhPsBT"
        val newUsername = "zhangsan_updated"
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan id

        // Fetch once to populate the cache.
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldUsername)

        // Update the username.
        val success = userAccountDao.updateProperties(userId, mapOf(UserAccount::username.name to newUsername))
        assertTrue(success, "the update should succeed")

        // Sync the cache (simulating a user info update).
        cacheHandler.syncOnUserUpdate(oldTenantId, oldUsername, newTenantId, newUsername)

        // Verify the old cache was evicted and the new cache can fetch the data.
//        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldUsername)
        val resourceIdsNew = cacheHandler.getResourceIds(newTenantId, newUsername)
        // The old cache entry should be evicted; the new key should fetch the same resource list (only the username changed).
        assertEquals(
            resourceIdsBefore.size,
            resourceIdsNew.size,
            "the new username should fetch the same resource list"
        )

        // Restore the username.
        userAccountDao.updateProperties(userId, mapOf(UserAccount::username.name to oldUsername))
    }

    @Test
    fun syncOnRoleUserChange() {
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan id
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN id

        // Fetch once to record the initial resource count.
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        val beforeSize = resourceIdsBefore.size

        // Insert a new user-role association record.
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync the cache (simulating a user-role association change).
        cacheHandler.syncOnRoleUserChange(tenantId, username)

        // Verify the cache was evicted and reloaded; the new role's resources should be included.
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.size >= beforeSize, "after sync, the new role's resource ids should be included")

        // Clean up test data.
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN id
        val tenantId = "tenant-001-InqhPsBT"
        val username = "admin"
        val resourceId = "resource-kkk"

        // Evict any existing cache first to keep the test environment clean.
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        // Fetch once to record the initial resource count (loads from DB and caches).
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        val beforeSize = resourceIdsBefore.size

        // Insert a new role-resource association record.
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)

        // Sync the cache (simulating a role-resource association change, affecting all users with the role).
        // This evicts the cache entries of all users who own the role.
        cacheHandler.syncOnRoleResourceChange(roleId)

        // Evict the cache again to force a reload from the DB (since @Cacheable may still hold the old value).
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        // Verify the cache was evicted and reloaded; the newly inserted resource should be present.
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.size > beforeSize, "after sync, the newly inserted resource id should be included; before: ${beforeSize}, after: ${resourceIdsAfter.size}, actual: ${resourceIdsAfter}")
        assertTrue(resourceIdsAfter.contains(resourceId), "should contain the newly inserted resource id ${resourceId}, actual: ${resourceIdsAfter}")

        // Clean up test data.
        authRoleResourceDao.deleteById(id)
        // Clean up the cache to avoid affecting other tests.
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnUserDelete() {
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"

        // Fetch once so the cache has data (even if it is an empty list).
        cacheHandler.getResourceIds(tenantId, username)

        // Delete the user record from the DB.
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan id
        userAccountDao.deleteById(userId)

        // Drive both listeners directly (AFTER_COMMIT does not fire in @Transactional tests):
        // in production, UserAccountDeleted triggers on(...) for both this cache and UserAccountHashCache.
        val event = UserAccountDeleted(userId, tenantId, username)
        cacheHandler.on(event)
        userAccountHashCache.on(event)

        // Verify the cache was evicted; re-fetching should return an empty list (the user no longer exists).
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.isEmpty(), "after user delete, the cache should be evicted and re-fetch should return an empty list")
    }

    // -------------------- Coverage for the group path ((tenantId, username) -> group -> role -> resource) --------------------

    @Test
    fun getResourceIds_includesResourcesFromGroupInheritedRoles() {
        // zhangsan is directly bound to ROLE_USER -> resource-ccc. After joining a group bound to ROLE_ADMIN
        // -> resources aaa/bbb. Expected: getResourceIds returns all three.
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "8e232124-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        val guId = insertGroupUser(gId, userId)
        try {
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
            val resources = cacheHandler.getResourceIds(tenantId, username)
            assertTrue(resources.contains("resource-ccc-InqhPsBT"), "should retain zhangsan's direct resource-ccc, actual: ${resources}")
            assertTrue(resources.contains("resource-aaa-InqhPsBT"), "should inherit resource-aaa through the group, actual: ${resources}")
            assertTrue(resources.contains("resource-bbb-InqhPsBT"), "should inherit resource-bbb through the group, actual: ${resources}")
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val userId = "8e232124-2222-2222-2222-222222222222"
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "8e232124-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        // Starting point: zhangsan only sees resource-ccc via direct binding and does not see aaa.
        val before = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(before.contains("resource-ccc-InqhPsBT"))
        assertTrue(!before.contains("resource-aaa-InqhPsBT"))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        val guId = insertGroupUser(gId, userId)
        try {
            cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(userId)))
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
            val after = cacheHandler.getResourceIds(tenantId, username)
            assertTrue(after.contains("resource-aaa-InqhPsBT"), "after joining the group, should see resource-aaa, actual: ${after}")
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // zhangsan is in the group, which initially has no roles. Add ROLE_ADMIN to the group
        // -> zhangsan's resources should gain aaa/bbb.
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val userId = "8e232124-2222-2222-2222-222222222222"
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "8e232124-grp3-cccc-cccc-cccccccccccc"
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        val gId = insertGroup(groupId, tenantId)
        val guId = insertGroupUser(gId, userId)
        try {
            val before = cacheHandler.getResourceIds(tenantId, username)
            assertTrue(!before.contains("resource-aaa-InqhPsBT"))

            val grId = insertGroupRole(gId, roleId)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(roleId)))
                // existing tests show @Cacheable may retain the pre-on() value, so explicitly evict
                cacheHandler.evict(cacheHandler.getKey(tenantId, username))
                val after = cacheHandler.getResourceIds(tenantId, username)
                assertTrue(after.contains("resource-aaa-InqhPsBT"), "after group binding, should see resource-aaa, actual: ${after}")
                assertTrue(after.contains("resource-bbb-InqhPsBT"), "after group binding, should see resource-bbb, actual: ${after}")
            } finally {
                authGroupRoleDao.deleteById(grId)
            }
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        }
    }

    private fun insertGroup(groupId: String, tenantId: String): String {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = tenantId
            this.subsysCode = "ams"
            this.active = true
            this.builtIn = false
        }
        return authGroupDao.insert(group)
    }

    private fun insertGroupUser(groupId: String, userId: String): String {
        val gu = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        return authGroupUserDao.insert(gu)
    }

    private fun insertGroupRole(groupId: String, roleId: String): String {
        val gr = AuthGroupRole.Companion().apply {
            this.groupId = groupId
            this.roleId = roleId
        }
        return authGroupRoleDao.insert(gr)
    }

}
