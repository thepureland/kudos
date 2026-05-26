package io.kudos.ms.auth.core.group.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByTenantIdAndGroupCodeCacheHandler
 *
 * Test data source: `UserIdsByTenantIdAndGroupCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByTenantIdAndGroupCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByTenantIdAndGroupCodeCache

    @Resource
    private lateinit var authGroupHashCache: AuthGroupHashCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun getUserIds() {
        // Existing tenant and group
        var tenantId = "tenant-001-58TWQx6c"
        var groupCode = "GROUP_ADMIN"
        val userIds2 = cacheHandler.getUserIds(tenantId, groupCode)
        val userIds3 = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // Non-existent group
        groupCode = "GROUP_NO_EXIST"
        val userIds4 = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIds4.isEmpty())

        // Non-existent tenant
        tenantId = "no_exist_tenant"
        groupCode = "GROUP_ADMIN"
        val userIds5 = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIds5.isEmpty())
    }

    @Test
    fun syncOnGroupUserInsert() {
        val tenantId = "tenant-001-58TWQx6c"
        val groupCode = "GROUP_USER"
        val groupId = "20796e8c-2222-2222-2222-222222222222" // ID of GROUP_USER
        val userId = "20796e8c-3333-3333-3333-333333333333"

        // Fetch once to record the initial user count
        val userIdsBefore = cacheHandler.getUserIds(tenantId, groupCode)
        val beforeSize = userIdsBefore.size

        // Insert a new group-user relation record
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // Sync cache (simulating group-user relation insertion)
        cacheHandler.syncOnGroupUserInsert(tenantId, groupCode)

        // Verify cache has been cleared and reloaded; should contain the newly inserted user
        val userIdsAfter = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIdsAfter.size > beforeSize, "After sync, should contain the newly inserted user ID")
        assertTrue(userIdsAfter.contains(userId), "Should contain the newly inserted user ID")

        // Clean up test data
        authGroupUserDao.deleteById(id)
    }

    @Test
    fun syncOnGroupUserDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val groupCode = "GROUP_USER"
        val groupId = "20796e8c-2222-2222-2222-222222222222" // ID of GROUP_USER
        val userId = "20796e8c-3333-3333-3333-333333333333"

        // First insert a group-user relation record
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // Sync cache first to ensure the newly inserted data is in the cache
        cacheHandler.syncOnGroupUserInsert(tenantId, groupCode)

        // Fetch once to ensure data is in the cache
        val userIdsBefore = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIdsBefore.contains(userId), "The newly inserted user relation should be in the cache")

        // Delete the database record
        val deleteSuccess = authGroupUserDao.deleteById(id)
        assertTrue(deleteSuccess, "Deletion should succeed")

        // Sync cache (simulating group-user relation deletion)
        cacheHandler.syncOnGroupUserDelete(tenantId, groupCode)

        // Verify cache has been cleared and reloaded; should not contain the deleted user
        val userIdsAfter = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(!userIdsAfter.contains(userId), "After deletion, the cache should be cleared and should not contain the deleted user ID")
    }

    @Test
    fun syncOnGroupUpdate() {
        val oldTenantId = "tenant-001-58TWQx6c"
        val oldGroupCode = "GROUP_USER"
        val newTenantId = "tenant-001-58TWQx6c"
        val newGroupCode = "GROUP_USER_UPDATED"
        val groupId = "20796e8c-2222-2222-2222-222222222222" // ID of GROUP_USER

        // Fetch once to ensure data is in the cache
        val userIdsBefore = cacheHandler.getUserIds(oldTenantId, oldGroupCode)

        // Update group code
        val group = authGroupDao.get(groupId)
        assertTrue(group != null, "Group should exist")
        val success = authGroupDao.updateProperties(groupId, mapOf(AuthGroup::code.name to newGroupCode))
        assertTrue(success, "Update should succeed")

        // Sync cache (simulating group info update)
        cacheHandler.syncOnGroupUpdate(oldTenantId, oldGroupCode, newTenantId, newGroupCode)

        // Verify the old cache has been cleared and the new cache can fetch data
        val userIdsNew = cacheHandler.getUserIds(newTenantId, newGroupCode)
        // The old cache should be cleared and the new cache should fetch data (user relations unchanged, only group code changed)
        assertEquals(
            userIdsBefore.size,
            userIdsNew.size,
            "The new group code should fetch the same user list"
        )

        // Restore group code
        authGroupDao.updateProperties(groupId, mapOf(AuthGroup::code.name to oldGroupCode))
    }

    @Test
    fun syncOnGroupDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val groupCode = "GROUP_USER"

        // Fetch once to ensure data is in the cache (even if empty list)
        cacheHandler.getUserIds(tenantId, groupCode)

        // Delete the group record from the database
        val groupId = "20796e8c-2222-2222-2222-222222222222" // ID of GROUP_USER
        authGroupDao.deleteById(groupId)

        // Directly drive both listeners (AFTER_COMMIT does not fire in @Transactional tests):
        // In production, the AuthGroupDeleted event triggers both this cache and AuthGroupHashCache's on(...).
        val event = AuthGroupDeleted(groupId, tenantId, groupCode)
        cacheHandler.on(event)
        authGroupHashCache.on(event)

        // Verify cache has been cleared; fetching again should return an empty list (since the group no longer exists)
        val userIdsAfter = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIdsAfter.isEmpty(), "After deleting the group, the cache should be cleared and fetching again should return an empty list")
    }

}
