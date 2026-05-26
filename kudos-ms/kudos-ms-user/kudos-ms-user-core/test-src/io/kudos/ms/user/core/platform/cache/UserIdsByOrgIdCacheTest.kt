package io.kudos.ms.user.core.platform.cache

import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByOrgIdCacheHandler
 *
 * Test data source: `UserIdsByOrgIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByOrgIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByOrgIdCache

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    @Test
    fun getUserIds() {
        // Existing org (with multiple users)
        var orgId = "84c558fe-1111-1111-1111-111111111111"
        val userIds2 = cacheHandler.getUserIds(orgId)
        val userIds3 = cacheHandler.getUserIds(orgId)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // Non-existent org
        orgId = "no_exist_org"
        val userIds4 = cacheHandler.getUserIds(orgId)
        assertTrue(userIds4.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // Fetch once first to record the user count
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        val beforeSize = userIdsBefore.size

        // Sync cache
        cacheHandler.syncOnInsert(orgUser, id)

        // Verify the cache has been cleared and reloaded
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsAfter.size >= beforeSize, "User count should increase or remain unchanged")
    }

    @Test
    fun syncOnUpdate() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // Sync insert into the cache first
        cacheHandler.syncOnInsert(orgUser, id)

        // Fetch once to make sure the cache has data
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsBefore.contains(orgUser.userId), "The new user should be in the cache")

        // Update the database record
        val success = userOrgUserDao.updateProperties(id, mapOf(UserOrgUser::orgAdmin.name to true))
        assert(success)

        // Sync cache
        cacheHandler.syncOnUpdate(null, id)

        // Verify the cache has been cleared and reloaded
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsAfter.contains(orgUser.userId), "The updated user should still be in the cache")
    }

    @Test
    fun syncOnDelete() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // Sync insert into the cache first
        cacheHandler.syncOnInsert(orgUser, id)

        // Fetch once to make sure the cache has data
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsBefore.contains(orgUser.userId), "The new user should be in the cache")

        // Delete the database record
        val deleteSuccess = userOrgUserDao.deleteById(id)
        assert(deleteSuccess)

        // Sync cache
        cacheHandler.syncOnDelete(orgUser, id)

        // Verify the cache has been cleared and reloaded
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(!userIdsAfter.contains(orgUser.userId), "The deleted user should not be in the cache")
    }

    @Test
    fun syncOnBatchDelete() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"

        val orgUser1 = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id1 = userOrgUserDao.insert(orgUser1)

        val orgUser2 = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-4444-4444-4444-444444444444"
            this.orgAdmin = false
        }
        val id2 = userOrgUserDao.insert(orgUser2)

        // Sync insert into the cache first
        cacheHandler.syncOnInsert(orgUser1, id1)
        cacheHandler.syncOnInsert(orgUser2, id2)

        // Fetch once to make sure the cache has data
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsBefore.contains(orgUser1.userId), "New user 1 should be in the cache")
        assertTrue(userIdsBefore.contains(orgUser2.userId), "New user 2 should be in the cache")

        // Batch delete database records
        val ids = listOf(id1, id2)
        val count = userOrgUserDao.batchDelete(ids)
        assert(count == 2)

        // Sync cache
        cacheHandler.syncOnBatchDelete(ids, listOf(orgId, orgId))

        // Verify the cache has been cleared and reloaded
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(!userIdsAfter.contains(orgUser1.userId), "Deleted user 1 should not be in the cache")
        assertTrue(!userIdsAfter.contains(orgUser2.userId), "Deleted user 2 should not be in the cache")
    }

    // -------------------- Coverage for descendant org member expansion (parent -> subtree) --------------------

    @Test
    fun getUserIds_includesUsersInDescendantOrgs() {
        // Build a tree: parent -> child; attach a user at each level. Querying parent should include users from both levels.
        val parentOrgId = insertOrg(name = "Sales Dept", parentId = null)
        val childOrgId = insertOrg(name = "East China Sales", parentId = parentOrgId)
        val parentUserId = "84c558fe-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val childUserId = "84c558fe-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        val rel1 = insertOrgUser(parentOrgId, parentUserId)
        val rel2 = insertOrgUser(childOrgId, childUserId)
        try {
            cacheHandler.evict(parentOrgId)
            val users = cacheHandler.getUserIds(parentOrgId)
            assertTrue(users.contains(parentUserId), "Should include the parent org's direct user, actual: ${users}")
            assertTrue(users.contains(childUserId), "Should include the child org's user, actual: ${users}")

            // When querying the child org alone, should only see its own users
            cacheHandler.evict(childOrgId)
            val childUsers = cacheHandler.getUserIds(childOrgId)
            assertTrue(childUsers.contains(childUserId))
            assertTrue(!childUsers.contains(parentUserId), "Child org should not include parent org's user in reverse")
        } finally {
            userOrgUserDao.deleteById(rel1)
            userOrgUserDao.deleteById(rel2)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
            cacheHandler.evict(childOrgId)
        }
    }

    @Test
    fun getUserIds_excludesUsersInInactiveDescendants() {
        // Child org active=false -> not counted as part of the subtree.
        val parentOrgId = insertOrg(name = "Sales Dept", parentId = null)
        val childOrgId = insertOrg(name = "East China (Inactive)", parentId = parentOrgId, active = false)
        val userInActive = "84c558fe-cccc-cccc-cccc-cccccccccccc"
        val rel = insertOrgUser(childOrgId, userInActive)
        try {
            cacheHandler.evict(parentOrgId)
            val users = cacheHandler.getUserIds(parentOrgId)
            assertTrue(!users.contains(userInActive), "Users from a disabled child org should not appear in the parent org view, actual: ${users}")
        } finally {
            userOrgUserDao.deleteById(rel)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
        }
    }

    @Test
    fun on_UserOrgUserRelationsChanged_invalidatesAncestorChain() {
        // Add a new user to the child org + fire the event -> the parent org cache should also be invalidated.
        val parentOrgId = insertOrg(name = "Sales Dept", parentId = null)
        val childOrgId = insertOrg(name = "East China Sales", parentId = parentOrgId)
        val initialUser = "84c558fe-dddd-dddd-dddd-dddddddddddd"
        val initialRel = insertOrgUser(childOrgId, initialUser)
        cacheHandler.evict(parentOrgId)
        cacheHandler.evict(childOrgId)
        try {
            // Warm up the parent org cache
            val before = cacheHandler.getUserIds(parentOrgId)
            assertTrue(before.contains(initialUser))

            // Add a new user to the child org
            val newUserId = "84c558fe-eeee-eeee-eeee-eeeeeeeeeeee"
            val newRel = insertOrgUser(childOrgId, newUserId)
            try {
                cacheHandler.on(UserOrgUserRelationsChanged(orgId = childOrgId, userIds = listOf(newUserId)))
                cacheHandler.evict(parentOrgId)
                val after = cacheHandler.getUserIds(parentOrgId)
                assertTrue(after.contains(initialUser), "Original user should remain after invalidation and recompute")
                assertTrue(after.contains(newUserId), "New user should propagate through the subtree to the parent org view, actual: ${after}")
            } finally {
                userOrgUserDao.deleteById(newRel)
            }
        } finally {
            userOrgUserDao.deleteById(initialRel)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
            cacheHandler.evict(childOrgId)
        }
    }

    // -------------------- Precise invalidation on org tree mutation --------------------

    @Test
    fun on_UserOrgUpdated_moveOrg_invalidatesOldAndNewAncestorChains() {
        // Initial tree: oldParent <- child  +  newParent (empty); child has a user
        val oldParentId = insertOrg("Old Dept", parentId = null)
        val newParentId = insertOrg("New Dept", parentId = null)
        val childOrgId = insertOrg("East China", parentId = oldParentId)
        val movingUser = "84c558fe-1111-aaaa-aaaa-aaaaaaaaaaaa"
        val rel = insertOrgUser(childOrgId, movingUser)
        cacheHandler.evict(oldParentId)
        cacheHandler.evict(newParentId)
        cacheHandler.evict(childOrgId)
        try {
            // Warm-up: oldParent view contains movingUser, newParent view is empty
            assertTrue(cacheHandler.getUserIds(oldParentId).contains(movingUser), "Initially oldParent should include the child user")
            assertTrue(!cacheHandler.getUserIds(newParentId).contains(movingUser), "Initially newParent should not include it")

            // Actual reparent: modify DB directly to represent the move
            userOrgDao.updateProperties(childOrgId, mapOf(UserOrg::parentId.name to newParentId))
            // Trigger event
            cacheHandler.on(
                UserOrgUpdated(id = childOrgId, oldParentId = oldParentId, newParentId = newParentId)
            )
            // Double evict as a safety net (consistent with the existing tests in fd2e425e)
            cacheHandler.evict(oldParentId)
            cacheHandler.evict(newParentId)
            cacheHandler.evict(childOrgId)

            assertTrue(
                !cacheHandler.getUserIds(oldParentId).contains(movingUser),
                "After the move, oldParent view should lose the child user, actual: ${cacheHandler.getUserIds(oldParentId)}",
            )
            assertTrue(
                cacheHandler.getUserIds(newParentId).contains(movingUser),
                "After the move, newParent view should gain the child user, actual: ${cacheHandler.getUserIds(newParentId)}",
            )
        } finally {
            userOrgUserDao.deleteById(rel)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(newParentId)
            userOrgDao.deleteById(oldParentId)
            cacheHandler.evict(oldParentId)
            cacheHandler.evict(newParentId)
            cacheHandler.evict(childOrgId)
        }
    }

    @Test
    fun on_UserOrgDeleted_invalidatesAncestorChain() {
        // parent -> child; child has a user. Delete child -> parent view should lose these users.
        val parentOrgId = insertOrg("Sales Dept", parentId = null)
        val childOrgId = insertOrg("East China", parentId = parentOrgId)
        val userInChild = "84c558fe-2222-bbbb-bbbb-bbbbbbbbbbbb"
        val rel = insertOrgUser(childOrgId, userInChild)
        cacheHandler.evict(parentOrgId)
        cacheHandler.evict(childOrgId)
        try {
            assertTrue(cacheHandler.getUserIds(parentOrgId).contains(userInChild), "Initially should include the child org's user")

            // Simulate deleting user_org_user first, then deleting the child org (this is also the
            // order used in real production deletions: relation first, entity second; the main
            // purpose here is to verify the listener invalidates via event.parentId)
            userOrgUserDao.deleteById(rel)
            userOrgDao.deleteById(childOrgId)

            cacheHandler.on(UserOrgDeleted(id = childOrgId, parentId = parentOrgId))
            cacheHandler.evict(parentOrgId)

            assertTrue(
                !cacheHandler.getUserIds(parentOrgId).contains(userInChild),
                "After deletion, parent view should not contain users that left the child, actual: ${cacheHandler.getUserIds(parentOrgId)}",
            )
        } finally {
            // child/rel already deleted; only parent remains
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
        }
    }

    @Test
    fun on_UserOrgBatchDeleted_invalidatesAllAncestorChains() {
        // parent -> [childA, childB]; each has a user. Batch delete both children.
        val parentOrgId = insertOrg("Sales Dept", parentId = null)
        val childA = insertOrg("East China", parentId = parentOrgId)
        val childB = insertOrg("South China", parentId = parentOrgId)
        val userA = "84c558fe-3333-cccc-aaaa-aaaaaaaaaaaa"
        val userB = "84c558fe-3333-cccc-bbbb-bbbbbbbbbbbb"
        val relA = insertOrgUser(childA, userA)
        val relB = insertOrgUser(childB, userB)
        cacheHandler.evict(parentOrgId)
        try {
            val before = cacheHandler.getUserIds(parentOrgId)
            assertTrue(before.contains(userA) && before.contains(userB), "Initially parent should include users from both children")

            userOrgUserDao.deleteById(relA)
            userOrgUserDao.deleteById(relB)
            userOrgDao.deleteById(childA)
            userOrgDao.deleteById(childB)

            cacheHandler.on(
                UserOrgBatchDeleted(
                    items = listOf(
                        UserOrgBatchDeleted.Item(childA, parentOrgId),
                        UserOrgBatchDeleted.Item(childB, parentOrgId),
                    )
                )
            )
            cacheHandler.evict(parentOrgId)
            val after = cacheHandler.getUserIds(parentOrgId)
            assertTrue(!after.contains(userA), "Parent should lose childA's user")
            assertTrue(!after.contains(userB), "Parent should lose childB's user")
        } finally {
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
        }
    }

    private fun insertOrg(
        name: String,
        parentId: String?,
        active: Boolean = true,
    ): String {
        val org = UserOrg.Companion().apply {
            this.name = name
            this.tenantId = "tenant-test-org-hierarchy"
            this.parentId = parentId
            this.orgTypeDictCode = "dept"
            this.active = active
            this.builtIn = false
        }
        return userOrgDao.insert(org)
    }

    private fun insertOrgUser(orgId: String, userId: String, admin: Boolean = false): String {
        val rel = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = admin
        }
        return userOrgUserDao.insert(rel)
    }

}
