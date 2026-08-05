package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.datascope.dao.AuthRoleScopeDao
import io.kudos.ms.auth.core.role.exclusion.dao.AuthRoleExclusionDao
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for the role-delete cascade in AuthRoleService.
 *
 * Verifies the security fix: deleting a role must reclaim every grant it carried — the
 * `auth_role_user` / `auth_role_resource` / `auth_group_role` / `auth_role_org` /
 * `auth_role_exclusion` relation rows are removed in the same transaction, so the deleted
 * role's permissions cannot keep resolving from orphan rows.
 *
 * Test data source: `AuthRoleDeleteCascadeTest.sql`
 *  - roleA: held directly by user1, inherited by user2 via group1, grants res1, has a data-scope
 *    org grant and an SoD exclusion pair (roleA, roleC)
 *  - roleB: held directly by user1, grants res2 (exercised via batchDelete)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleDeleteCascadeTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleService: IAuthRoleService

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var authRoleScopeDao: AuthRoleScopeDao

    @Resource
    private lateinit var authRoleExclusionDao: AuthRoleExclusionDao

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    private val roleA = "3fae12cd-0000-0000-0000-0000000000a1"
    private val roleB = "3fae12cd-0000-0000-0000-0000000000a2"
    private val roleC = "3fae12cd-0000-0000-0000-0000000000a3"
    private val user1 = "3fae12cd-0000-0000-0000-0000000000b1"
    private val user2 = "3fae12cd-0000-0000-0000-0000000000b2"
    private val res1 = "3fae12cd-0000-0000-0000-0000000000e1"
    private val group1 = "3fae12cd-0000-0000-0000-0000000000d1"

    @Test
    fun deleteById_cascadesAllRelationRows() {
        // Fixture sanity: every relation kind points at roleA before deletion.
        assertTrue(authRoleUserDao.searchUserIdsByRoleId(roleA).contains(user1))
        assertTrue(authRoleResourceDao.searchResourceIdsByRoleIds(listOf(roleA)).contains(res1))
        assertTrue(authGroupRoleDao.searchGroupIdsByRoleId(roleA).contains(group1))
        assertTrue(authRoleScopeDao.searchValuesByRoleId(roleA).isNotEmpty())
        assertTrue(authRoleExclusionDao.searchByRoleIds(listOf(roleA)).isNotEmpty())

        assertTrue(authRoleService.deleteById(roleA))

        // Every relation row pointing at the deleted role is gone.
        assertTrue(authRoleUserDao.searchUserIdsByRoleId(roleA).isEmpty())
        assertTrue(authRoleResourceDao.searchResourceIdsByRoleIds(listOf(roleA)).isEmpty())
        assertTrue(authGroupRoleDao.searchGroupIdsByRoleId(roleA).isEmpty())
        assertTrue(authRoleScopeDao.searchValuesByRoleId(roleA).isEmpty())
        assertTrue(authRoleExclusionDao.searchByRoleIds(listOf(roleA)).isEmpty())

        // Unrelated grants survive: roleB keeps its user, and the exclusion partner role itself remains.
        assertTrue(authRoleUserDao.searchUserIdsByRoleId(roleB).contains(user1))
        assertNotNull(authRoleService.getRoleRecord(roleC))
    }

    /**
     * Cache invalidation is driven by hand here, and that is not a shortcut.
     *
     * `deleteById` publishes the relation-cleanup events (the caches subscribe to them in
     * production), but they are `@TransactionalEventListener(AFTER_COMMIT)` and this test's
     * transaction is rolled back — the commit never happens, so the listeners never run. Asserting
     * on the effective sets without evicting would therefore be testing Spring's delivery phase
     * rather than the cascade. Same convention as `RoleIdsByUserIdCacheTest`.
     */
    @Test
    fun deleteById_revokesEffectivePermissionsImmediately() {
        assertTrue(authRoleService.deleteById(roleA))
        evictEffectiveSets(user1, user2)

        // Direct holder: the deleted role and its resources drop out of the effective sets.
        assertFalse(authRoleService.getUserRoleIds(user1).contains(roleA))
        assertFalse(authRoleService.getUserResourceIds(user1).contains(res1))

        // Group-inherited holder: the group no longer confers the deleted role.
        assertFalse(authRoleService.getUserRoleIds(user2).contains(roleA))
        assertFalse(authRoleService.getUserResourceIds(user2).contains(res1))

        // The other role is unaffected.
        assertTrue(authRoleService.getUserRoleIds(user1).contains(roleB))
    }

    @Test
    fun batchDelete_cascadesRelationRows() {
        assertTrue(authRoleUserDao.searchUserIdsByRoleId(roleB).contains(user1))

        assertEquals(1, authRoleService.batchDelete(listOf(roleB)))
        evictEffectiveSets(user1)

        assertTrue(authRoleUserDao.searchUserIdsByRoleId(roleB).isEmpty())
        assertTrue(authRoleResourceDao.searchResourceIdsByRoleIds(listOf(roleB)).isEmpty())
        assertFalse(authRoleService.getUserRoleIds(user1).contains(roleB))

        // roleA's relations are untouched by deleting roleB.
        assertTrue(authRoleUserDao.searchUserIdsByRoleId(roleA).contains(user1))
    }

    /**
     * Delivers, by hand, the very event `deleteById` publishes — see the KDoc on
     * [deleteById_revokesEffectivePermissionsImmediately] for why the container will not.
     *
     * Calling the listener rather than reaching into the cache is deliberate: it keeps the real
     * invalidation logic under test, and only the delivery mechanism is stubbed out.
     */
    private fun evictEffectiveSets(vararg userIds: String) {
        val affected = userIds.toList()
        roleIdsByUserIdCache.on(AuthRoleUserRelationsChanged(roleA, affected))
        resourceIdsByUserIdCache.on(AuthRoleUserRelationsChanged(roleA, affected))
    }
}
