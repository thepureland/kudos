package io.kudos.ms.auth.core.role.datascope.cache

import io.kudos.ms.auth.core.role.datascope.dao.AuthRoleScopeDao
import io.kudos.ms.auth.core.role.datascope.event.AuthRoleScopeChanged
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleScope
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for DataScopeByUserIdCache.
 *
 * Test data source: `DataScopeByUserIdCacheTest.sql`
 * Org tree: root(d0) <- child(d1) <- grand(d2); plus standalone other(d3).
 *
 * What these lock down is the pair of properties a cache on the *query* path has to have at once:
 * it must actually stop the resolution work from happening per query, and it must still be right
 * after a configuration change. Testing either alone would pass while the other silently fails.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DataScopeByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthRoleDataScopeService

    @Resource
    private lateinit var cache: DataScopeByUserIdCache

    @Resource
    private lateinit var scopeDao: AuthRoleScopeDao

    private val orgChild = "6b8b6d1f-0000-0000-0000-0000000000d1"
    private val orgOther = "6b8b6d1f-0000-0000-0000-0000000000d3"

    /** The CUSTOM-scoped role held by [userCustom]. */
    private val roleCustom = "6b8b6d1f-0000-0000-0000-0000000000f4"
    private val userCustom = "6b8b6d1f-0000-0000-0000-0000000000e4"
    private val userOrg = "6b8b6d1f-0000-0000-0000-0000000000e2"

    /**
     * The point of the cache: a second resolution must not re-read `auth_role_scope`.
     *
     * Proven by changing the grant through the DAO, which publishes nothing — the same thing an
     * uninstrumented write looks like. If resolution still went to the database per call, the new
     * value would show up here and this would fail.
     */
    @Test
    fun resolveIsServedFromCacheRatherThanResolvedPerQuery() {
        assertEquals(setOf(orgOther), service.resolveUserDataScope(userCustom).valuesOf("org"))

        val added = scopeDao.insert(AuthRoleScope {
            this.roleId = roleCustom
            this.dimension = "org"
            this.scopeValue = orgChild
        })
        try {
            assertEquals(
                setOf(orgOther),
                service.resolveUserDataScope(userCustom).valuesOf("org"),
                "a cached scope must not be recomputed per query",
            )
            // ...and the uncached path proves the row really did land, so the assertion above is
            // about caching rather than about a write that never happened.
            assertEquals(
                setOf(orgOther, orgChild),
                service.computeUserDataScope(userCustom).valuesOf("org"),
            )
        } finally {
            scopeDao.deleteById(added)
        }
    }

    /**
     * The other half: a scope edit must reach the cached value. `bindScope` publishes
     * [AuthRoleScopeChanged] for exactly this, and the listener expands it to every holder of the
     * role and of its descendants.
     */
    @Test
    fun scopeGrantChangeInvalidatesEveryHolder() {
        assertEquals(setOf(orgOther), service.resolveUserDataScope(userCustom).valuesOf("org"))

        service.bindScope(roleCustom, "org", listOf(orgChild))

        // AFTER_COMMIT listeners do not fire inside these rolled-back tests, so the eviction the
        // event triggers is driven directly — same convention as RoleIdsByUserIdCacheTest.
        cache.on(AuthRoleScopeChanged(roleCustom))

        assertEquals(
            setOf(orgChild),
            service.resolveUserDataScope(userCustom).valuesOf("org"),
            "the edit an admin just made must be what the next query filters by",
        )
    }

    /**
     * `selfOnly()` and other narrow outcomes must be cached like any other. They are what the
     * restricted population resolves to, so an `unless`-style guard that skipped them would leave
     * precisely the users this cache exists for paying full resolution on every query.
     */
    @Test
    fun narrowOutcomesAreCachedToo() {
        val fallback = service.resolveUserDataScope("no-such-user-at-all")
        assertFalse(fallback.all)
        assertTrue(fallback.self, "the restrictive fallback still applies")
        assertEquals(fallback, cache.getDataScope("no-such-user-at-all"))

        // A plain ORG subject resolves to its own org and stays stable across calls.
        val org = service.resolveUserDataScope(userOrg)
        assertEquals(setOf(orgChild), org.valuesOf("org"))
        assertEquals(org, service.resolveUserDataScope(userOrg))
    }
}
