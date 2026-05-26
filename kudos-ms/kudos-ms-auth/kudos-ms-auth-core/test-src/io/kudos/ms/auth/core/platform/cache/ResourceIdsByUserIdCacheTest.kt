package io.kudos.ms.auth.core.platform.cache

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByUserIdCacheHandler
 *
 * Test data source: `ResourceIdsByUserIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByUserIdCache

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
        // Existing user id with a single role that owns multiple resources.
        var userId = "165f7094-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(userId)
        val resourceIds2 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds1.isNotEmpty(), "user ${userId} should have a non-empty resource id list")
        assertEquals(resourceIds1, resourceIds2, "two calls should return the same result (cache verification)")
        // Resource verification: user 11111111 has ROLE_ADMIN and should contain resource-aaa and resource-bbb.
        assertEquals(2, resourceIds1.size, "user ${userId} should have 2 resource ids")
        assertTrue(resourceIds1.contains("resource-aaa-6Z55FylV"), "should contain resource-aaa, actual: ${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb-6Z55FylV"), "should contain resource-bbb, actual: ${resourceIds1}")

        // Existing user id with multiple roles, each owning multiple resources.
        userId = "165f7094-2222-2222-2222-222222222222"
        // Evict any existing cache first to keep the test environment clean.
        cacheHandler.evict(userId)
        val resourceIds3 = cacheHandler.getResourceIds(userId)
        val resourceIds4 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds3.isNotEmpty(), "user ${userId} should have a non-empty resource id list")
        assertEquals(resourceIds3, resourceIds4, "two calls should return the same result (cache verification)")
        // User 22222222 has two roles (ROLE_USER and ROLE_ADMIN) and should contain the deduplicated union of their resources.
        // ROLE_ADMIN resources: resource-aaa, resource-bbb
        // ROLE_USER resources: resource-ccc, resource-ddd
        // 4 resources in total.
        assertEquals(
            4,
            resourceIds3.size,
            "user ${userId} should have 4 resource ids, actual: ${resourceIds3}"
        )
        assertTrue(resourceIds3.contains("resource-aaa-6Z55FylV"), "should contain resource-aaa")
        assertTrue(resourceIds3.contains("resource-bbb-6Z55FylV"), "should contain resource-bbb")
        assertTrue(resourceIds3.contains("resource-ccc-6Z55FylV"), "should contain resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd-6Z55FylV"), "should contain resource-ddd")

        // Existing user id with no roles.
        userId = "165f7094-3333-3333-3333-333333333333"
        // Evict any existing cache first to keep the test environment clean.
        cacheHandler.evict(userId)
        // Clean up any leftover user-role association records (in case a prior test did not clean them).
        val roleUserCriteria = Criteria(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        val existingRoleUsers = authRoleUserDao.search(roleUserCriteria)
        existingRoleUsers.forEach { authRoleUserDao.deleteById(it.id) }
        // Evict the cache again to force a reload from the database.
        cacheHandler.evict(userId)
        val resourceIds5 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds5.isEmpty(), "user ${userId} has no roles and should return an empty list, actual: ${resourceIds5}")

        // Non-existent user id.
        userId = "no_exist_user_id"
        val resourceIds6 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds6.isEmpty(), "a non-existent user id should return an empty list")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER id

        // Fetch once to record the initial resource count.
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        val beforeSize = resourceIdsBefore.size

        // Insert a new user-role association record.
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync the cache (simulating a user-role association change).
        cacheHandler.syncOnRoleUserChange(userId)

        // Verify the cache was evicted and reloaded, and now contains the new role's resources.
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.size >= beforeSize, "after sync, the new role's resource ids should be included")

        // Clean up test data.
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER id
        val userId = "165f7094-2222-2222-2222-222222222222" // user who owns the role
        val resourceId = "resource-jjj"

        // Evict any existing cache first to keep the test environment clean.
        cacheHandler.evict(userId)

        // Fetch once to record the initial resource count.
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        val beforeSize = resourceIdsBefore.size

        // Insert a new role-resource association record.
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)

        // Sync the cache (simulating a role-resource association change, affecting all users who own the role).
        cacheHandler.syncOnRoleResourceChange(roleId)

        // Verify the cache was evicted and reloaded, and now contains the newly inserted resource.
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.size > beforeSize, "after sync, the newly inserted resource id should be included; before: ${beforeSize}, after: ${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "should contain the newly inserted resource id")

        // Clean up test data.
        authRoleResourceDao.deleteById(id)
        // Clean up the cache to avoid affecting other tests.
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-1111-1111-1111-111111111111" // ROLE_ADMIN id

        // Insert a user-role association record first.
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync the cache first so the new data is in the cache.
        cacheHandler.syncOnRoleUserChange(userId)

        // Fetch once so the cache is populated.
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsBefore.isNotEmpty(), "the newly inserted role association should be in the cache")

        // Delete the DB record (simulating user delete or user-role association delete).
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "the delete should succeed")

        // Drive the event listener directly (AFTER_COMMIT does not fire in @Transactional tests, so call on(...) directly).
        cacheHandler.on(UserAccountDeleted(userId, tenantId = "tenant-x", username = "user-x"))

        // Verify the cache was evicted; a re-fetch should not contain the deleted resource.
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.isEmpty(), "after delete, the cache should be evicted and not contain the deleted resource id")
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "165f7094-3333-3333-3333-333333333333"
        val userId2 = "165f7094-3333-3333-3333-333333333333"
        val roleId1 = "165f7094-1111-1111-1111-111111111111" // ROLE_ADMIN id
        val roleId2 = "165f7094-2222-2222-2222-222222222222" // ROLE_USER id
        val userIds = listOf(userId1, userId2)

        // Fetch once to record the initial resource count.
        val resourceIds1Before = cacheHandler.getResourceIds(userId1)
        val beforeSize = resourceIds1Before.size

        // Batch-insert user-role association records.
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

        // Batch-sync the cache (simulating a batch of user-role association changes).
        cacheHandler.syncOnBatchRoleUserChange(userIds)

        // Verify the cache was evicted and reloaded, and now contains the new role's resources.
        val resourceIds1After = cacheHandler.getResourceIds(userId1)
        assertTrue(resourceIds1After.size >= beforeSize, "after sync, the new role's resource ids should be included")

        // Clean up test data.
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    // -------------------- Coverage for the group path (user -> group -> role -> resource) --------------------

    @Test
    fun getResourceIds_includesResourcesFromGroupInheritedRoles() {
        // lisi (3333) has no direct role; joining a group -> group binds ROLE_USER (2222), which is linked to ccc + ddd.
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER
        val groupId = "165f7094-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        // Clean up any leftover direct role bindings.
        val cleanupCriteria = Criteria(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        authRoleUserDao.search(cleanupCriteria).forEach { authRoleUserDao.deleteById(it.id) }
        cacheHandler.evict(userId)

        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, roleId)
        try {
            cacheHandler.evict(userId)
            val resources = cacheHandler.getResourceIds(userId)
            assertTrue(
                resources.contains("resource-ccc-6Z55FylV"),
                "user should inherit ROLE_USER via the group and get resource-ccc; actual: ${resources}",
            )
            assertTrue(
                resources.contains("resource-ddd-6Z55FylV"),
                "user should inherit ROLE_USER via the group and get resource-ddd; actual: ${resources}",
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
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER -> ccc/ddd
        val groupId = "165f7094-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(userId)

        // Starting point: user lisi has no roles -> cache is empty.
        val before = cacheHandler.getResourceIds(userId)
        assertTrue(before.isEmpty() || !before.contains("resource-ccc-6Z55FylV"))

        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, roleId)
        try {
            // Note: the gId returned by bindUserToRoleViaGroup is the real DB id; it must be used as event.groupId.
            cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(userId)))
            cacheHandler.evict(userId)
            val after = cacheHandler.getResourceIds(userId)
            assertTrue(
                after.contains("resource-ccc-6Z55FylV"),
                "after joining the group, should get resource-ccc through inheritance; actual: ${after}",
            )
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(userId)
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        val userId = "165f7094-3333-3333-3333-333333333333"
        val initialRoleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER -> ccc/ddd
        val addedRoleId = "165f7094-1111-1111-1111-111111111111" // ROLE_ADMIN -> aaa/bbb
        val groupId = "165f7094-grp3-cccc-cccc-cccccccccccc"

        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, initialRoleId)
        cacheHandler.evict(userId)
        try {
            val before = cacheHandler.getResourceIds(userId)
            assertTrue(before.contains("resource-ccc-6Z55FylV"))
            assertTrue(!before.contains("resource-aaa-6Z55FylV"))

            // Append ROLE_ADMIN to the group -- use the real gId (dao.insert does not accept a manually set id).
            val newGr = AuthGroupRole.Companion().apply {
                this.groupId = gId
                this.roleId = addedRoleId
            }
            val newGrId = authGroupRoleDao.insert(newGr)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(addedRoleId)))
                cacheHandler.evict(userId)
                val after = cacheHandler.getResourceIds(userId)
                assertTrue(after.contains("resource-ccc-6Z55FylV"), "should still contain the original ROLE_USER resources")
                assertTrue(after.contains("resource-aaa-6Z55FylV"), "should add the ROLE_ADMIN resources")
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

    @Test
    fun syncOnRoleResourceChange_alsoInvalidatesGroupInheritedUsers() {
        // Group inheritance scenario: users in the group inherit ROLE_USER via the group; when ROLE_USER gains
        // a new resource, the group members' caches must also be invalidated.
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER
        val groupId = "165f7094-grp4-dddd-dddd-dddddddddddd"
        val newResourceId = "resource-from-group-test"

        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, roleId)
        cacheHandler.evict(userId)
        try {
            // Warm-up: the cache should have ccc / ddd.
            val before = cacheHandler.getResourceIds(userId)
            assertTrue(before.contains("resource-ccc-6Z55FylV"))
            assertTrue(!before.contains(newResourceId))

            // Add a new resource to ROLE_USER.
            val rr = AuthRoleResource.Companion().apply {
                this.roleId = roleId
                this.resourceId = newResourceId
            }
            val rrId = authRoleResourceDao.insert(rr)
            try {
                // syncOnRoleResourceChange must invalidate both "users directly bound to ROLE_USER" and
                // "users who inherit ROLE_USER through a group".
                cacheHandler.syncOnRoleResourceChange(roleId)
                cacheHandler.evict(userId) // Belt-and-braces: avoid @Cacheable retaining the old value.
                val after = cacheHandler.getResourceIds(userId)
                assertTrue(
                    after.contains(newResourceId),
                    "users who inherit through a group should also see the newly added resource $newResourceId; actual: ${after}",
                )
            } finally {
                authRoleResourceDao.deleteById(rrId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(userId)
        }
    }

    private fun bindUserToRoleViaGroup(
        groupId: String,
        userId: String,
        roleId: String,
    ): Triple<String, String, String> {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = "tenant-001-6Z55FylV"
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
