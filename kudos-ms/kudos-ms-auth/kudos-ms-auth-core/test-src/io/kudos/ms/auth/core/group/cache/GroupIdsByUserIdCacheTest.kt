package io.kudos.ms.auth.core.group.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for GroupIdsByUserIdCacheHandler
 *
 * Test data source: `GroupIdsByUserIdCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class GroupIdsByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: GroupIdsByUserIdCache

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun getGroupIds() {
        // Existing user ID, has one group
        var userId = "88207878-1111-1111-1111-111111111111"
        val groupIds1 = cacheHandler.getGroupIds(userId)
        val groupIds2 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds1.isNotEmpty(), "User ${userId} should have a list of group IDs")
        assertEquals(groupIds1, groupIds2, "Two calls should return the same result (cache verification)")
        // Verify group ID: user 11111111 has one group GROUP_ADMIN
        assertEquals(1, groupIds1.size, "User ${userId} should have 1 group ID")
        assertTrue(groupIds1.contains("88307878-1111-1111-1111-111111111111"), "Should contain the group ID of GROUP_ADMIN; actual returned: ${groupIds1}")

        // Existing user ID, has multiple groups
        userId = "88207878-2222-2222-2222-222222222222"
        val groupIds3 = cacheHandler.getGroupIds(userId)
        val groupIds4 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds3.isNotEmpty(), "User ${userId} should have a list of group IDs")
        assertEquals(groupIds3, groupIds4, "Two calls should return the same result (cache verification)")
        // User 22222222 has two groups (GROUP_USER and GROUP_ADMIN)
        assertEquals(
            2,
            groupIds3.size,
            "User ${userId} should have 2 group IDs; actual returned: ${groupIds3}"
        )
        assertTrue(groupIds3.contains("88307878-1111-1111-1111-111111111111"), "Should contain the group ID of GROUP_ADMIN")
        assertTrue(groupIds3.contains("88307878-2222-2222-2222-222222222222"), "Should contain the group ID of GROUP_USER")

        // Existing user ID, but no groups
        userId = "88207878-3333-3333-3333-333333333333"
        val groupIds5 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds5.isEmpty(), "User ${userId} has no groups, should return empty list")

        // Non-existent user ID
        userId = "no_exist_user_id"
        val groupIds6 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds6.isEmpty(), "Non-existent user ID should return empty list")
    }

    @Test
    fun syncOnGroupUserChange() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val groupId = "88307878-2222-2222-2222-222222222222" // ID of GROUP_USER

        // Fetch once to record the initial group count
        val groupIdsBefore = cacheHandler.getGroupIds(userId)
        val beforeSize = groupIdsBefore.size

        // Insert a new user-group relation record
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // Sync cache (simulating user-group relation change)
        cacheHandler.syncOnGroupUserChange(userId)

        // Verify cache has been cleared and reloaded; should contain the newly inserted group
        val groupIdsAfter = cacheHandler.getGroupIds(userId)
        assertTrue(groupIdsAfter.size > beforeSize, "After sync, should contain the newly inserted group ID")
        assertTrue(groupIdsAfter.contains(groupId), "Should contain the newly inserted group ID")

        // Clean up test data
        authGroupUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchGroupUserChange() {
        val userId1 = "88207878-3333-3333-3333-333333333333"
        val userId2 = "88207878-3333-3333-3333-333333333333"
        val groupId1 = "88307878-1111-1111-1111-111111111111" // ID of GROUP_ADMIN
        val groupId2 = "88307878-2222-2222-2222-222222222222" // ID of GROUP_USER
        val userIds = listOf(userId1, userId2)

        // Fetch once to record the initial group count
        val groupIds1Before = cacheHandler.getGroupIds(userId1)
        val beforeSize = groupIds1Before.size

        // Batch insert user-group relation records
        val authGroupUser1 = AuthGroupUser.Companion().apply {
            this.groupId = groupId1
            this.userId = userId1
        }
        val id1 = authGroupUserDao.insert(authGroupUser1)

        val authGroupUser2 = AuthGroupUser.Companion().apply {
            this.groupId = groupId2
            this.userId = userId2
        }
        val id2 = authGroupUserDao.insert(authGroupUser2)

        // Batch sync cache (simulating batch user-group relation changes)
        cacheHandler.syncOnBatchGroupUserChange(userIds)

        // Verify cache has been cleared and reloaded; should contain newly inserted groups
        val groupIds1After = cacheHandler.getGroupIds(userId1)
        assertTrue(groupIds1After.size > beforeSize, "After sync, should contain the newly inserted group ID")

        // Clean up test data
        authGroupUserDao.deleteById(id1)
        authGroupUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val groupId = "88307878-1111-1111-1111-111111111111" // ID of GROUP_ADMIN

        // First insert a user-group relation record
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // Sync cache first to ensure the newly inserted data is in the cache
        cacheHandler.syncOnGroupUserChange(userId)

        // Fetch once to ensure data is in the cache
        val groupIdsBefore = cacheHandler.getGroupIds(userId)
        assertTrue(groupIdsBefore.contains(groupId), "The newly inserted group relation should be in the cache")

        // Delete the database record (simulating user deletion or user-group relation deletion)
        val deleteSuccess = authGroupUserDao.deleteById(id)
        assertTrue(deleteSuccess, "Deletion should succeed")

        // Sync cache (simulating user deletion)
        cacheHandler.on(UserAccountDeleted(userId, tenantId = "tenant-x", username = "user-x"))

        // Verify cache has been cleared; fetching again should not contain the deleted group
        val groupIdsAfter = cacheHandler.getGroupIds(userId)
        assertTrue(!groupIdsAfter.contains(groupId), "After deletion, the cache should be cleared and should not contain the deleted group ID")
    }

}
