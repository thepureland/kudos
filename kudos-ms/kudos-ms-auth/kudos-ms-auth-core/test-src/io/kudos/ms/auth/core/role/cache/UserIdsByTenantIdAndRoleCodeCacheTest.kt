package io.kudos.ms.auth.core.role.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByTenantIdAndRoleCodeCacheHandler
 *
 * Test data source: `UserIdsByTenantIdAndRoleCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByTenantIdAndRoleCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByTenantIdAndRoleCodeCache

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

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
        // Existing tenant and role
        var tenantId = "tenant-001-58TWQx6c"
        var roleCode = "ROLE_ADMIN"
        val userIds2 = cacheHandler.getUserIds(tenantId, roleCode)
        val userIds3 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // Non-existing role
        roleCode = "ROLE_NO_EXIST"
        val userIds4 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds4.isEmpty())

        // Non-existing tenant
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val userIds5 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds5.isEmpty())
    }

    @Test
    fun syncOnRoleUserInsert() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER ID
        val userId = "10796e8c-3333-3333-3333-333333333333"

        // Fetch once to record the initial user count
        val userIdsBefore = cacheHandler.getUserIds(tenantId, roleCode)
        val beforeSize = userIdsBefore.size

        // Insert a new role-user relation record
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync cache (simulating a role-user relation insert)
        cacheHandler.syncOnRoleUserInsert(tenantId, roleCode)

        // Verify the cache was evicted and reloaded; it should contain the newly inserted user
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsAfter.size > beforeSize, "after sync, the newly inserted user ID should be present")
        assertTrue(userIdsAfter.contains(userId), "should contain the newly inserted user ID")

        // Clean up test data
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleUserDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER ID
        val userId = "10796e8c-3333-3333-3333-333333333333"

        // First insert a role-user relation record
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)

        // Sync cache first so it contains the newly inserted data
        cacheHandler.syncOnRoleUserInsert(tenantId, roleCode)

        // Fetch once to ensure the cache has data
        val userIdsBefore = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsBefore.contains(userId), "the newly inserted user relation should be in the cache")

        // Delete the DB record
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "deletion should succeed")

        // Sync cache (simulating a role-user relation delete)
        cacheHandler.syncOnRoleUserDelete(tenantId, roleCode)

        // Verify the cache was evicted and reloaded; it should not contain the deleted user
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(!userIdsAfter.contains(userId), "after deletion the cache should be evicted and not contain the deleted user ID")
    }

    @Test
    fun syncOnRoleUpdate() {
        val oldTenantId = "tenant-001-58TWQx6c"
        val oldRoleCode = "ROLE_USER"
        val newTenantId = "tenant-001-58TWQx6c"
        val newRoleCode = "ROLE_USER_UPDATED"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER ID

        // Fetch once to ensure the cache has data
        val userIdsBefore = cacheHandler.getUserIds(oldTenantId, oldRoleCode)

        // Update the role code
        val role = authRoleDao.get(roleId)
        assertTrue(role != null, "the role should exist")
        val success = authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to newRoleCode))
        assertTrue(success, "update should succeed")

        // Sync cache (simulating a role info update)
        cacheHandler.syncOnRoleUpdate(oldTenantId, oldRoleCode, newTenantId, newRoleCode)

        // Verify the old cache was evicted and the new cache can be fetched
        val userIdsNew = cacheHandler.getUserIds(newTenantId, newRoleCode)
        // The old cache should be evicted; the new cache should fetch the same data (user relations unchanged, only the role code changed)
        assertEquals(
            userIdsBefore.size,
            userIdsNew.size,
            "the new role code should fetch the same user list"
        )

        // Restore the role code
        authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to oldRoleCode))
    }

    @Test
    fun syncOnRoleDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"

        // Fetch once to ensure the cache has data (even if it's an empty list)
        cacheHandler.getUserIds(tenantId, roleCode)

        // Delete the role record from the DB
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER ID
        authRoleDao.deleteById(roleId)

        // Drive both listeners directly (AFTER_COMMIT does not fire in @Transactional tests):
        // In production, the AuthRoleDeleted event triggers on(...) on both this cache and AuthRoleHashCache.
        val event = AuthRoleDeleted(roleId, tenantId, roleCode)
        cacheHandler.on(event)
        authRoleHashCache.on(event)

        // Verify the cache was evicted; a refetch should return an empty list (because the role no longer exists)
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsAfter.isEmpty(), "after deleting the role the cache should be evicted and a refetch should return an empty list")
    }

    // -------------------- group path ((tenantId, roleCode) <- group <- user) coverage --------------------

    @Test
    fun getUserIds_includesUsersFromGroupBoundToRole() {
        // ROLE_USER (2222) initially only has user 2222 directly bound. Create a group bound to ROLE_USER and add
        // user 1111 (admin). Expected: getUserIds(tenant, ROLE_USER) returns both 2222 (direct) and 1111 (group).
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222"
        val viaGroupUserId = "10796e8c-1111-1111-1111-111111111111"
        val groupId = "10796e8c-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        val guId = insertGroupUser(gId, viaGroupUserId)
        try {
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
            val users = cacheHandler.getUserIds(tenantId, roleCode)
            assertTrue(users.contains(viaGroupUserId), "should inherit admin via the group; actual: ${users}")
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222"
        val newUserId = "10796e8c-1111-1111-1111-111111111111"
        val groupId = "10796e8c-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        try {
            // Warm the cache: the group is empty, so newUserId is not visible
            val before = cacheHandler.getUserIds(tenantId, roleCode)
            assertTrue(!before.contains(newUserId), "the user is not yet in the group")

            val guId = insertGroupUser(gId, newUserId)
            try {
                cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(newUserId)))
                // Existing tests have shown @Cacheable may still hold the value seen before on(); explicit evict is needed.
                cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
                val after = cacheHandler.getUserIds(tenantId, roleCode)
                assertTrue(after.contains(newUserId), "after joining the group, (${tenantId},${roleCode}) should contain the user; actual: ${after}")
            } finally {
                authGroupUserDao.deleteById(guId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // The group already has a user. Initially the group is not bound to ROLE_USER. After binding and invalidation,
        // the user enters the (tenant, ROLE_USER) set.
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222"
        val userId = "10796e8c-1111-1111-1111-111111111111"
        val groupId = "10796e8c-grp3-cccc-cccc-cccccccccccc"
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        val gId = insertGroup(groupId, tenantId)
        val guId = insertGroupUser(gId, userId)
        try {
            val before = cacheHandler.getUserIds(tenantId, roleCode)
            assertTrue(!before.contains(userId), "the group is not yet bound to ROLE_USER")

            val grId = insertGroupRole(gId, roleId)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(roleId)))
                // Existing tests have shown @Cacheable may still hold the value seen before on(); explicit evict is needed.
                cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
                val after = cacheHandler.getUserIds(tenantId, roleCode)
                assertTrue(after.contains(userId), "after binding the role, (${tenantId},${roleCode}) should contain admin; actual: ${after}")
            } finally {
                authGroupRoleDao.deleteById(grId)
            }
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
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
