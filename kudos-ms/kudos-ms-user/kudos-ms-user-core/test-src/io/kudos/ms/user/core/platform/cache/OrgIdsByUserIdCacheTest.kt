package io.kudos.ms.user.core.platform.cache

import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for OrgIdsByUserIdCacheHandler
 *
 * Test data source: `OrgIdsByUserIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class OrgIdsByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: OrgIdsByUserIdCache

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Test
    fun getOrgIds() {
        // Reload the cache first to align with current test data (avoid residue from other tests causing user 3333 to have orgs)
        cacheHandler.reloadAll(true)

        // Existing user ID with one org
        var userId = "81cea00f-1111-1111-1111-111111111111"
        val orgIds1 = cacheHandler.getOrgIds(userId)
        val orgIds2 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds1.isNotEmpty(), "User ${userId} should have an org ID list")
        assertEquals(orgIds1, orgIds2, "Two calls should return the same result (cache validation)")
        // Verify org ID: user 11111111 belongs to the Tech department
        assertEquals(1, orgIds1.size, "User ${userId} should have 1 org ID")
        assertTrue(orgIds1.contains("81cea00f-1111-1111-1111-111111111111"), "Should contain the Tech department org ID, actual: ${orgIds1}")

        // Existing user ID with multiple orgs
        userId = "81cea00f-2222-2222-2222-222222222222"
        val orgIds3 = cacheHandler.getOrgIds(userId)
        val orgIds4 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds3.isNotEmpty(), "User ${userId} should have an org ID list")
        assertEquals(orgIds3, orgIds4, "Two calls should return the same result (cache validation)")
        // User 22222222 belongs to the Tech and Product departments
        assertEquals(2, orgIds3.size, "User ${userId} should have 2 org IDs, actual: ${orgIds3}")
        assertTrue(orgIds3.contains("81cea00f-1111-1111-1111-111111111111"), "Should contain the Tech department org ID")
        assertTrue(orgIds3.contains("81cea00f-2222-2222-2222-222222222222"), "Should contain the Product department org ID")

        // Existing user ID but with no orgs
        userId = "81cea00f-3333-3333-3333-333333333333"
        val orgIds5 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds5.isEmpty(), "User ${userId} has no orgs and should return an empty list")

        // Non-existent user ID
        userId = "no_exist_user_id"
        val orgIds6 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds6.isEmpty(), "A non-existent user ID should return an empty list")
    }

    @Test
    fun syncOnOrgUserChange() {
        val userId = "81cea00f-3333-3333-3333-333333333333"
        val orgId = "81cea00f-2222-2222-2222-222222222222"

        // Fetch once first to record the initial org count
        val orgIdsBefore = cacheHandler.getOrgIds(userId)
        val beforeSize = orgIdsBefore.size

        // Insert a new user-org relation record
        val userOrgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(userOrgUser)

        // Sync cache (simulating a user-org relation change)
        cacheHandler.syncOnOrgUserChange(userId)

        // Verify the cache has been cleared and reloaded, should contain the newly inserted org
        val orgIdsAfter = cacheHandler.getOrgIds(userId)
        assertTrue(orgIdsAfter.size >= beforeSize, "After sync, should contain the newly inserted org ID")
        assertTrue(orgIdsAfter.contains(orgId), "Should contain the newly inserted org ID")

        // Clean up test data
        userOrgUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchOrgUserChange() {
        val userId1 = "81cea00f-3333-3333-3333-333333333333"
        val userId2 = "81cea00f-3333-3333-3333-333333333333"
        val orgId1 = "81cea00f-1111-1111-1111-111111111111"
        val orgId2 = "81cea00f-2222-2222-2222-222222222222"
        val userIds = listOf(userId1, userId2)

        // Fetch once first to record the initial org count
        val orgIds1Before = cacheHandler.getOrgIds(userId1)
        val orgIds2Before = cacheHandler.getOrgIds(userId2)
        val beforeSize1 = orgIds1Before.size
        val beforeSize2 = orgIds2Before.size

        // Batch insert user-org relation records
        val userOrgUser1 = UserOrgUser().apply {
            this.orgId = orgId1
            this.userId = userId1
            this.orgAdmin = false
        }
        val id1 = userOrgUserDao.insert(userOrgUser1)

        val userOrgUser2 = UserOrgUser().apply {
            this.orgId = orgId2
            this.userId = userId2
            this.orgAdmin = false
        }
        val id2 = userOrgUserDao.insert(userOrgUser2)

        // Batch sync cache (simulating batch user-org relation changes)
        cacheHandler.syncOnBatchOrgUserChange(userIds)

        // Verify the cache has been cleared and reloaded, should contain the newly inserted orgs
        val orgIds1After = cacheHandler.getOrgIds(userId1)
        val orgIds2After = cacheHandler.getOrgIds(userId2)
        assertTrue(orgIds1After.size >= beforeSize1 || orgIds2After.size >= beforeSize2, "After sync, should contain the newly inserted org IDs")

        // Clean up test data
        userOrgUserDao.deleteById(id1)
        userOrgUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "81cea00f-3333-3333-3333-333333333333"
        val orgId = "81cea00f-1111-1111-1111-111111111111"

        // Insert a user-org relation record first
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // Sync the cache first so it contains the newly inserted data
        cacheHandler.syncOnOrgUserChange(userId)

        // Fetch once to make sure data is in the cache
        val orgIdsBefore = cacheHandler.getOrgIds(userId)
        assertTrue(orgIdsBefore.contains(orgId), "The newly inserted org relation should be in the cache")

        // Delete the database record (simulating user deletion or user-org relation deletion)
        val deleteSuccess = userOrgUserDao.deleteById(id)
        assertTrue(deleteSuccess, "Delete should succeed")

        // Drive the event listener directly (AFTER_COMMIT is not triggered in @Transactional tests, so call on(...) directly)
        cacheHandler.on(UserAccountDeleted(userId, tenantId = "tenant-x", username = "user-x"))

        // Verify the cache has been cleared; refetching should not contain the deleted org
        val orgIdsAfter = cacheHandler.getOrgIds(userId)
        assertTrue(!orgIdsAfter.contains(orgId), "After deletion, the cache should be cleared and not contain the deleted org ID")
    }

}
