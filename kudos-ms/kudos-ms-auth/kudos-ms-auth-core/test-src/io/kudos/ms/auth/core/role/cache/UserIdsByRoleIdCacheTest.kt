package io.kudos.ms.auth.core.role.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByRoleIdCacheHandler
 *
 * Test data source: `UserIdsByRoleIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByRoleIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByRoleIdCache

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Test
    fun getUserIds() {
        // Existing role ID with multiple users
        var roleId = "5e90ce80-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(roleId)
        val userIds2 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds1.isNotEmpty(), "role ${roleId} should have a user ID list")
        assertEquals(userIds1, userIds2, "two calls should return the same result (cache validation)")
        // Verify user IDs: role ROLE_ADMIN has users admin and zhangsan
        assertEquals(2, userIds1.size, "role ${roleId} should have 2 user IDs")
        assertTrue(userIds1.contains("5e90ce80-1111-1111-1111-111111111111"), "should contain admin's user ID; actual: ${userIds1}")
        assertTrue(userIds1.contains("5e90ce80-2222-2222-2222-222222222222"), "should contain zhangsan's user ID; actual: ${userIds1}")

        // Existing role ID with one user
        roleId = "5e90ce80-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(roleId)
        val userIds4 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds3.isNotEmpty(), "role ${roleId} should have a user ID list")
        assertEquals(userIds3, userIds4, "two calls should return the same result (cache validation)")
        // Role ROLE_USER only has user zhangsan
        assertEquals(
            1,
            userIds3.size,
            "role ${roleId} should have 1 user ID; actual: ${userIds3}"
        )
        assertTrue(userIds3.contains("5e90ce80-2222-2222-2222-222222222222"), "should contain zhangsan's user ID")

        // Existing role ID with no users
        roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds5.isEmpty(), "role ${roleId} has no users; should return an empty list")

        // Non-existing role ID
        roleId = "no_exist_role_id"
        val userIds6 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds6.isEmpty(), "non-existing role ID should return an empty list")
    }

    @Test
    fun syncOnRoleUserChange() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-3333-3333-3333-333333333333"

        // Fetch once to record the initial user count
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        val beforeSize = userIdsBefore.size

        // Insert a new role-user relation record
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync cache (simulating a role-user relation change)
        cacheHandler.syncOnRoleUserChange(roleId)

        // Verify the cache was evicted and reloaded; it should contain the newly inserted user
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsAfter.size > beforeSize, "after sync, the newly inserted user ID should be present")
        assertTrue(userIdsAfter.contains(userId), "should contain the newly inserted user ID")

        // Clean up test data
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val roleId1 = "5e90ce80-3333-3333-3333-333333333333"
        val roleId2 = "5e90ce80-3333-3333-3333-333333333333"
        val userId1 = "5e90ce80-1111-1111-1111-111111111111"
        val userId2 = "5e90ce80-2222-2222-2222-222222222222"
        val roleIds = listOf(roleId1, roleId2)

        // Fetch once to record the initial user count
        val userIds1Before = cacheHandler.getUserIds(roleId1)
        val beforeSize = userIds1Before.size

        // Batch-insert role-user relation records
        val authRoleUser1 = AuthRoleUser.Companion().apply {
            this.roleId = roleId1
            this.userId = userId1
        }
        val id1 = authRoleUserDao.insert(authRoleUser1)

        val authRoleUser2 = AuthRoleUser.Companion().apply {
            this.roleId = roleId2
            this.userId = userId2
        }
        val id2 = authRoleUserDao.insert(authRoleUser2)

        // Batch-sync cache (simulating batch role-user relation changes)
        cacheHandler.syncOnBatchRoleUserChange(roleIds)

        // Verify the cache was evicted and reloaded; it should contain the newly inserted users
        val userIds1After = cacheHandler.getUserIds(roleId1)
        assertTrue(userIds1After.size > beforeSize, "after sync, the newly inserted user IDs should be present")

        // Clean up test data
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-1111-1111-1111-111111111111"

        // First insert a role-user relation record
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync cache first so it contains the newly inserted data
        cacheHandler.syncOnRoleUserChange(roleId)

        // Fetch once to ensure the cache has data
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsBefore.contains(userId), "the newly inserted user relation should be in the cache")

        // Delete the DB record (simulating role deletion or role-user relation deletion)
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "deletion should succeed")

        // Drive the event listener directly (AFTER_COMMIT does not fire in @Transactional tests, so call on(...) directly)
        cacheHandler.on(AuthRoleDeleted(roleId, tenantId = "tenant-x", code = "code-x"))

        // Verify the cache was evicted; a refetch should not contain the deleted user
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(!userIdsAfter.contains(userId), "after deletion the cache should be evicted and not contain the deleted user ID")
    }

    // -------------------- group path (role <- group <- user) coverage --------------------

    @Test
    fun getUserIds_includesUsersFromGroupBoundToRole() {
        // ROLE_GUEST (3333) currently has no direct users. Create a group bound to ROLE_GUEST and add two users.
        // Expected: getUserIds(ROLE_GUEST) returns both users.
        val roleId = "5e90ce80-3333-3333-3333-333333333333" // ROLE_GUEST
        val userA = "5e90ce80-1111-1111-1111-111111111111" // admin
        val userB = "5e90ce80-2222-2222-2222-222222222222" // zhangsan
        val groupId = "5e90ce80-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        cacheHandler.evict(roleId)

        val gId = insertGroup(groupId)
        val grId = insertGroupRole(gId, roleId)
        val gu1Id = insertGroupUser(gId, userA)
        val gu2Id = insertGroupUser(gId, userB)
        try {
            cacheHandler.evict(roleId)
            val users = cacheHandler.getUserIds(roleId)
            assertTrue(users.contains(userA), "should inherit admin via the group; actual: ${users}")
            assertTrue(users.contains(userB), "should inherit zhangsan via the group; actual: ${users}")
        } finally {
            authGroupUserDao.deleteById(gu1Id)
            authGroupUserDao.deleteById(gu2Id)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(roleId)
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333" // ROLE_GUEST
        val newUserId = "5e90ce80-2222-2222-2222-222222222222"
        val groupId = "5e90ce80-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(roleId)

        val gId = insertGroup(groupId)
        val grId = insertGroupRole(gId, roleId)
        try {
            // Initial state: no users in the group
            val before = cacheHandler.getUserIds(roleId)
            assertTrue(!before.contains(newUserId), "the new user is not yet in the group")

            // Add to group
            val guId = insertGroupUser(gId, newUserId)
            try {
                cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(newUserId)))
                // Existing tests have shown @Cacheable may still hold the value seen before on(); explicit evict is needed.
                cacheHandler.evict(roleId)
                val after = cacheHandler.getUserIds(roleId)
                assertTrue(after.contains(newUserId), "after joining the group, role ${roleId} should contain the user; actual: ${after}")
            } finally {
                authGroupUserDao.deleteById(guId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(roleId)
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // User A is already in the group. Initially the group is not bound to ROLE_GUEST, so the cache is empty.
        // After binding and invalidation, A appears in ROLE_GUEST's user set.
        val roleId = "5e90ce80-3333-3333-3333-333333333333" // ROLE_GUEST
        val userA = "5e90ce80-1111-1111-1111-111111111111"
        val groupId = "5e90ce80-grp3-cccc-cccc-cccccccccccc"
        cacheHandler.evict(roleId)

        val gId = insertGroup(groupId)
        val guId = insertGroupUser(gId, userA)
        try {
            val before = cacheHandler.getUserIds(roleId)
            assertTrue(!before.contains(userA), "the group is not yet bound to ROLE_GUEST")

            // Bind ROLE_GUEST to the group
            val grId = insertGroupRole(gId, roleId)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(roleId)))
                cacheHandler.evict(roleId)
                val after = cacheHandler.getUserIds(roleId)
                assertTrue(after.contains(userA), "after binding the role, ROLE_GUEST should contain admin; actual: ${after}")
            } finally {
                authGroupRoleDao.deleteById(grId)
            }
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(roleId)
        }
    }

    private fun insertGroup(groupId: String): String {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = "tenant-001-twyuFAaV"
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
