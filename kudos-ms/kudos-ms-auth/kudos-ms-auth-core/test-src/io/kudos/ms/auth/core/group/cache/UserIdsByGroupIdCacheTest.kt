package io.kudos.ms.auth.core.group.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByGroupIdCacheHandler
 *
 * Test data source: `UserIdsByGroupIdCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByGroupIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByGroupIdCache

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun getUserIds() {
        // Existing group ID, has multiple users
        var groupId = "6e90ce80-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(groupId)
        val userIds2 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds1.isNotEmpty(), "Group ${groupId} should have a list of user IDs")
        assertEquals(userIds1, userIds2, "Two calls should return the same result (cache verification)")
        // Verify user IDs: group GROUP_ADMIN has users admin and zhangsan
        assertEquals(2, userIds1.size, "Group ${groupId} should have 2 user IDs")
        assertTrue(userIds1.contains("5e90ce80-1111-1111-1111-111111111111"), "Should contain user ID of admin; actual returned: ${userIds1}")
        assertTrue(userIds1.contains("5e90ce80-2222-2222-2222-222222222222"), "Should contain user ID of zhangsan; actual returned: ${userIds1}")

        // Existing group ID, has one user
        groupId = "6e90ce80-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(groupId)
        val userIds4 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds3.isNotEmpty(), "Group ${groupId} should have a list of user IDs")
        assertEquals(userIds3, userIds4, "Two calls should return the same result (cache verification)")
        // Group GROUP_USER has only user zhangsan
        assertEquals(
            1,
            userIds3.size,
            "Group ${groupId} should have 1 user ID; actual returned: ${userIds3}"
        )
        assertTrue(userIds3.contains("5e90ce80-2222-2222-2222-222222222222"), "Should contain user ID of zhangsan")

        // Existing group ID, but no users
        groupId = "6e90ce80-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds5.isEmpty(), "Group ${groupId} has no users, should return empty list")

        // Non-existent group ID
        groupId = "no_exist_group_id"
        val userIds6 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds6.isEmpty(), "Non-existent group ID should return empty list")
    }

    @Test
    fun syncOnGroupUserChange() {
        val groupId = "6e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-3333-3333-3333-333333333333"

        // Fetch once to record the initial user count
        val userIdsBefore = cacheHandler.getUserIds(groupId)
        val beforeSize = userIdsBefore.size

        // Insert a new group-user relation record
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // Sync cache (simulating group-user relation change)
        cacheHandler.syncOnGroupUserChange(groupId)

        // Verify cache has been cleared and reloaded; should contain the newly inserted user
        val userIdsAfter = cacheHandler.getUserIds(groupId)
        assertTrue(userIdsAfter.size > beforeSize, "After sync, should contain the newly inserted user ID")
        assertTrue(userIdsAfter.contains(userId), "Should contain the newly inserted user ID")

        // Clean up test data
        authGroupUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchGroupUserChange() {
        val groupId1 = "6e90ce80-3333-3333-3333-333333333333"
        val groupId2 = "6e90ce80-3333-3333-3333-333333333333"
        val userId1 = "5e90ce80-1111-1111-1111-111111111111"
        val userId2 = "5e90ce80-2222-2222-2222-222222222222"
        val groupIds = listOf(groupId1, groupId2)

        // Fetch once to record the initial user count
        val userIds1Before = cacheHandler.getUserIds(groupId1)
        val beforeSize = userIds1Before.size

        // Batch insert group-user relation records
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

        // Batch sync cache (simulating batch group-user relation changes)
        cacheHandler.syncOnBatchGroupUserChange(groupIds)

        // Verify cache has been cleared and reloaded; should contain newly inserted users
        val userIds1After = cacheHandler.getUserIds(groupId1)
        assertTrue(userIds1After.size > beforeSize, "After sync, should contain the newly inserted user ID")

        // Clean up test data
        authGroupUserDao.deleteById(id1)
        authGroupUserDao.deleteById(id2)
    }

    @Test
    fun syncOnGroupDelete() {
        val groupId = "6e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-1111-1111-1111-111111111111"

        // First insert a group-user relation record
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // Sync cache first to ensure the newly inserted data is in the cache
        cacheHandler.syncOnGroupUserChange(groupId)

        // Fetch once to ensure data is in the cache
        val userIdsBefore = cacheHandler.getUserIds(groupId)
        assertTrue(userIdsBefore.contains(userId), "The newly inserted user relation should be in the cache")

        // Delete the database record (simulating group deletion or group-user relation deletion)
        val deleteSuccess = authGroupUserDao.deleteById(id)
        assertTrue(deleteSuccess, "Deletion should succeed")

        // Directly drive the event listener (AFTER_COMMIT does not fire in @Transactional tests, so invoke on(...) directly)
        cacheHandler.on(AuthGroupDeleted(groupId, tenantId = "tenant-x", code = "code-x"))

        // Verify cache has been cleared; fetching again should not contain the deleted user
        val userIdsAfter = cacheHandler.getUserIds(groupId)
        assertTrue(!userIdsAfter.contains(userId), "After deletion, the cache should be cleared and should not contain the deleted user ID")
    }

}
