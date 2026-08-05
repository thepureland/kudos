package io.kudos.ms.auth.core.role.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for [AuthRoleDao].
 *
 * Covers every query helper: searchActiveRolesForCache, searchRoleByTenantIdAndRoleCode (hit /
 * miss), searchActiveRoleIdsByTenantId (active-only filtering), searchAllRoleIdToParentIdForCache,
 * and the bidirectional graph walks searchAncestorRoleIds / searchDescendantRoleIds (including the
 * empty-input early-return, multi-generation walk, and depth cap).
 *
 * Test data source: `AuthRoleDaoTest.sql`
 *  - basic roles: 20/21 active in tenant t1, 22 inactive in tenant t2
 *  - parent chain: root(30) <- mid(31) <- leaf(32), plus sibling(33) under root
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    private val tenant1 = "auth-tenant-dao-test-1-DqEGww0j"
    private val tenant2 = "auth-tenant-dao-test-2-DqEGww0j"
    private val role1 = "7e4a2d1b-0000-0000-0000-000000000020"
    private val role2 = "7e4a2d1b-0000-0000-0000-000000000021"
    private val role3Inactive = "7e4a2d1b-0000-0000-0000-000000000022"
    private val root = "7e4a2d1b-0000-0000-0000-000000000030"
    private val mid = "7e4a2d1b-0000-0000-0000-000000000031"
    private val leaf = "7e4a2d1b-0000-0000-0000-000000000032"
    private val sibling = "7e4a2d1b-0000-0000-0000-000000000033"

    @Test
    fun searchActiveRolesForCache() {
        val active = authRoleDao.searchActiveRolesForCache()
        val ids = active.map { it.id }.toSet()
        assertTrue(ids.contains(role1), "active role 20 should be present")
        assertTrue(ids.contains(role2), "active role 21 should be present")
        assertFalse(ids.contains(role3Inactive), "inactive role 22 must be excluded")
        // every returned row really is active
        assertTrue(active.all { it.active == true }, "searchActiveRolesForCache must only return active=true rows")
    }

    @Test
    fun searchRoleByTenantIdAndRoleCode() {
        val hit = authRoleDao.searchRoleByTenantIdAndRoleCode(tenant1, "auth-role-dao-test-1-DqEGww0j")
        assertNotNull(hit)
        assertEquals(role1, hit.id)
        assertEquals(tenant1, hit.tenantId)

        // inactive role still found (the method ignores active state)
        val inactive = authRoleDao.searchRoleByTenantIdAndRoleCode(tenant2, "auth-role-dao-test-3-DqEGww0j")
        assertNotNull(inactive, "tenant+code lookup must ignore active state")
        assertEquals(role3Inactive, inactive.id)
        assertEquals(false, inactive.active)

        // wrong tenant, right code -> miss
        assertNull(authRoleDao.searchRoleByTenantIdAndRoleCode(tenant2, "auth-role-dao-test-1-DqEGww0j"))
        // right tenant, wrong code -> miss
        assertNull(authRoleDao.searchRoleByTenantIdAndRoleCode(tenant1, "no-such-code"))
    }

    @Test
    fun searchActiveRoleIdsByTenantId() {
        val t1Ids = authRoleDao.searchActiveRoleIdsByTenantId(tenant1)
        assertTrue(t1Ids.contains(role1))
        assertTrue(t1Ids.contains(role2))
        assertTrue(t1Ids.contains(root))
        // tenant2 only has an inactive role -> active filter yields empty
        val t2Ids = authRoleDao.searchActiveRoleIdsByTenantId(tenant2)
        assertFalse(t2Ids.contains(role3Inactive), "inactive role must be excluded")
        assertTrue(t2Ids.isEmpty(), "tenant2 has no active role; expected empty, got $t2Ids")
        // non-existent tenant -> empty
        assertTrue(authRoleDao.searchActiveRoleIdsByTenantId("no-such-tenant").isEmpty())
    }

    @Test
    fun searchAllRoleIdToParentIdForCache() {
        val map = authRoleDao.searchAllRoleIdToParentIdForCache()
        // only roles with a non-null parent appear
        assertEquals(root, map[mid])
        assertEquals(mid, map[leaf])
        assertEquals(root, map[sibling])
        // root itself has no parent -> absent from the map
        assertFalse(map.containsKey(root), "root has null parent_id and must not appear")
        assertFalse(map.containsKey(role1), "role with null parent_id must not appear")
    }

    @Test
    fun searchAncestorRoleIds_emptyInputReturnsEmpty() {
        assertTrue(authRoleDao.searchAncestorRoleIds(emptyList()).isEmpty())
    }

    @Test
    fun searchAncestorRoleIds_walksUpAndExcludesInputs() {
        // from leaf: ancestors are mid and root
        val ancestors = authRoleDao.searchAncestorRoleIds(setOf(leaf))
        assertEquals(setOf(mid, root), ancestors)
        // When mid is also an input, it is an ancestor of leaf but excluded because inputs are
        // removed from the result (documented contract: "excluding the inputs themselves").
        val combined = authRoleDao.searchAncestorRoleIds(setOf(leaf, mid))
        assertEquals(setOf(root), combined)
        assertFalse(combined.contains(leaf), "inputs are excluded from the ancestor set")
        assertFalse(combined.contains(mid), "mid is an input here, so it is excluded too")
    }

    @Test
    fun searchAncestorRoleIds_rootHasNoAncestors() {
        assertTrue(authRoleDao.searchAncestorRoleIds(setOf(root)).isEmpty())
    }

    @Test
    fun searchAncestorRoleIds_depthCapStopsWalk() {
        // maxDepth=1 only climbs one generation: from leaf -> mid (root unreached)
        val oneLevel = authRoleDao.searchAncestorRoleIds(setOf(leaf), maxDepth = 1)
        assertEquals(setOf(mid), oneLevel)
    }

    @Test
    fun searchDescendantRoleIds_walksDown() {
        // from root: descendants are mid, leaf, sibling
        val descendants = authRoleDao.searchDescendantRoleIds(root)
        assertEquals(setOf(mid, leaf, sibling), descendants)
        // root itself is not a descendant of itself
        assertFalse(descendants.contains(root))
    }

    @Test
    fun searchDescendantRoleIds_leafHasNone() {
        assertTrue(authRoleDao.searchDescendantRoleIds(leaf).isEmpty())
    }

    @Test
    fun searchDescendantRoleIds_depthCapStopsWalk() {
        // maxDepth=1 from root only reaches direct children mid and sibling (leaf unreached)
        val oneLevel = authRoleDao.searchDescendantRoleIds(root, maxDepth = 1)
        assertEquals(setOf(mid, sibling), oneLevel)
    }
}
