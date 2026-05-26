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
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for RoleIdsByUserIdCacheHandler
 *
 * Test data source: `RoleIdsByUserIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleIdsByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleIdsByUserIdCache

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Test
    fun getRoleIds() {
        // Existing user ID with one role
        var userId = "88207878-1111-1111-1111-111111111111"
        val roleIds1 = cacheHandler.getRoleIds(userId)
        val roleIds2 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds1.isNotEmpty(), "user ${userId} should have a role ID list")
        assertEquals(roleIds1, roleIds2, "two calls should return the same result (cache validation)")
        // Verify role ID: user 11111111 has role ROLE_ADMIN
        assertEquals(1, roleIds1.size, "user ${userId} should have 1 role ID")
        assertTrue(roleIds1.contains("88207878-1111-1111-1111-111111111111"), "should contain ROLE_ADMIN's role ID; actual: ${roleIds1}")

        // Existing user ID with multiple roles
        userId = "88207878-2222-2222-2222-222222222222"
        val roleIds3 = cacheHandler.getRoleIds(userId)
        val roleIds4 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds3.isNotEmpty(), "user ${userId} should have a role ID list")
        assertEquals(roleIds3, roleIds4, "two calls should return the same result (cache validation)")
        // User 22222222 has two roles (ROLE_USER and ROLE_ADMIN)
        assertEquals(
            2,
            roleIds3.size,
            "user ${userId} should have 2 role IDs; actual: ${roleIds3}"
        )
        assertTrue(roleIds3.contains("88207878-1111-1111-1111-111111111111"), "should contain ROLE_ADMIN's role ID")
        assertTrue(roleIds3.contains("88207878-2222-2222-2222-222222222222"), "should contain ROLE_USER's role ID")

        // Existing user ID with no roles
        userId = "88207878-3333-3333-3333-333333333333"
        val roleIds5 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds5.isEmpty(), "user ${userId} has no roles; should return an empty list")

        // Non-existing user ID
        userId = "no_exist_user_id"
        val roleIds6 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds6.isEmpty(), "non-existing user ID should return an empty list")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER ID

        // Fetch once to record the initial role count
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        val beforeSize = roleIdsBefore.size

        // Insert a new user-role relation record
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync cache (simulating a user-role relation change)
        cacheHandler.syncOnRoleUserChange(userId)

        // Verify the cache was evicted and reloaded; it should contain the newly inserted role
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsAfter.size > beforeSize, "after sync, the newly inserted role ID should be present")
        assertTrue(roleIdsAfter.contains(roleId), "should contain the newly inserted role ID")

        // Clean up test data
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "88207878-3333-3333-3333-333333333333"
        val userId2 = "88207878-3333-3333-3333-333333333333"
        val roleId1 = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN ID
        val roleId2 = "88207878-2222-2222-2222-222222222222" // ROLE_USER ID
        val userIds = listOf(userId1, userId2)

        // Fetch once to record the initial role count
        val roleIds1Before = cacheHandler.getRoleIds(userId1)
        val beforeSize = roleIds1Before.size

        // Batch-insert user-role relation records
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

        // Batch-sync cache (simulating batch user-role relation changes)
        cacheHandler.syncOnBatchRoleUserChange(userIds)

        // Verify the cache was evicted and reloaded; it should contain the newly inserted role
        val roleIds1After = cacheHandler.getRoleIds(userId1)
        assertTrue(roleIds1After.size > beforeSize, "after sync, the newly inserted role ID should be present")

        // Clean up test data
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN ID

        // First insert a user-role relation record
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync cache first so the cache contains the newly inserted data
        cacheHandler.syncOnRoleUserChange(userId)

        // Fetch once to ensure the cache has data
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsBefore.contains(roleId), "the newly inserted role relation should be in the cache")

        // Delete the DB record (simulating user deletion or user-role relation deletion)
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "deletion should succeed")

        // Drive the event listener directly (AFTER_COMMIT does not fire in @Transactional tests, so call on(...) directly)
        cacheHandler.on(UserAccountDeleted(userId, tenantId = "tenant-x", username = "user-x"))

        // Verify the cache was evicted; a refetch should not contain the deleted role
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(!roleIdsAfter.contains(roleId), "after deletion the cache should be evicted and not contain the deleted role ID")
    }

    // -------------------- group path (user -> group -> role) coverage --------------------

    @Test
    fun getRoleIds_includesGroupInheritedRoles() {
        // lisi (user 3333) has no directly bound roles, but joining the group inherits ROLE_USER (2222)
        // because the group is bound to ROLE_USER.
        // Expected: getRoleIds returns a set including ROLE_USER.
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER
        val groupId = "88207878-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        cacheHandler.evict(userId)
        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, roleId)
        try {
            cacheHandler.evict(userId)
            val roleIds = cacheHandler.getRoleIds(userId)
            assertTrue(
                roleIds.contains(roleId),
                "user ${userId} should inherit role ${roleId} via group ${groupId}; actual: ${roleIds}",
            )
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(userId)
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        // Initial state: lisi is directly bound to ROLE_USER, so the cache contains ROLE_USER.
        // Trigger: lisi is added to a group that is bound to ROLE_ADMIN, then fire AuthGroupUserRelationsChanged.
        // Expected: after recomputation the cache contains both ROLE_USER (direct) and ROLE_ADMIN (via group).
        val userId = "88207878-3333-3333-3333-333333333333"
        val directRoleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER
        val groupRoleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "88207878-grp2-bbbb-bbbb-bbbbbbbbbbbb"

        val directRel = AuthRoleUser.Companion().apply {
            this.roleId = directRoleId
            this.userId = userId
        }
        val directRelId = authRoleUserDao.insert(directRel)
        cacheHandler.evict(userId)
        try {
            // Warm the cache: only the direct role is visible
            val before = cacheHandler.getRoleIds(userId)
            assertTrue(before.contains(directRoleId), "initial cache should contain the direct role")
            assertTrue(!before.contains(groupRoleId), "group inheritance is not yet in effect")

            // Join the group -> role binding
            val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, groupRoleId)
            try {
                // Drive the listener directly
                cacheHandler.on(AuthGroupUserRelationsChanged(groupId, listOf(userId)))
                val after = cacheHandler.getRoleIds(userId)
                assertTrue(after.contains(directRoleId), "after invalidation the direct role should still be present")
                assertTrue(after.contains(groupRoleId), "after invalidation the group-inherited role should be present")
            } finally {
                authGroupRoleDao.deleteById(grId)
                authGroupUserDao.deleteById(guId)
                authGroupDao.deleteById(gId)
            }
        } finally {
            authRoleUserDao.deleteById(directRelId)
            cacheHandler.evict(userId)
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // The user is already in the group, which is initially bound to ROLE_USER; the cache sees ROLE_USER.
        // The group then also binds ROLE_ADMIN and fires AuthGroupRoleRelationsChanged -> the cache should contain both.
        val userId = "88207878-3333-3333-3333-333333333333"
        val initialRoleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER
        val addedRoleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "88207878-grp3-cccc-cccc-cccccccccccc"

        // Create group + add user + bind group to ROLE_USER
        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, initialRoleId)
        cacheHandler.evict(userId)
        try {
            val before = cacheHandler.getRoleIds(userId)
            assertTrue(before.contains(initialRoleId), "initial cache should inherit ROLE_USER via the group")
            assertTrue(!before.contains(addedRoleId), "ROLE_ADMIN is not yet bound to the group")

            // Also bind ROLE_ADMIN to the same group. Note that the gId returned by bindUserToRoleViaGroup is the
            // real DB id; dao.insert does not accept a manually set id, so we must pass gId rather than the locally
            // predefined groupId string.
            val newGr = AuthGroupRole.Companion().apply {
                this.groupId = gId
                this.roleId = addedRoleId
            }
            val newGrId = authGroupRoleDao.insert(newGr)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(addedRoleId)))
                cacheHandler.evict(userId)
                val after = cacheHandler.getRoleIds(userId)
                assertTrue(after.contains(initialRoleId), "after invalidation the original group role should still be present")
                assertTrue(after.contains(addedRoleId), "after invalidation the newly added group role should be present")
            } finally {
                authGroupRoleDao.deleteById(newGrId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(userId)
        }
    }

    /**
     * Test helper: create a group, add the user to it, and bind the role to the group. Returns
     * (groupId, groupUserId, groupRoleId) for finally-cleanup. tenantId / subsysCode are kept consistent
     * with the SQL fixture (same tenant + ams subsystem) for realism.
     */
    private fun bindUserToRoleViaGroup(
        groupId: String,
        userId: String,
        roleId: String,
    ): Triple<String, String, String> {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = "tenant-001-Gv4Pb40w"
            this.subsysCode = "ams"
            this.active = true
            this.builtIn = false
        }
        val gId = authGroupDao.insert(group)
        val gu = AuthGroupUser.Companion().apply {
            this.groupId = gId
            this.userId = userId
        }
        val guId = authGroupUserDao.insert(gu)
        val gr = AuthGroupRole.Companion().apply {
            this.groupId = gId
            this.roleId = roleId
        }
        val grId = authGroupRoleDao.insert(gr)
        return Triple(gId, guId, grId)
    }

}
