package io.kudos.ms.auth.core.role.exclusion.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for [AuthRoleExclusionDao].
 *
 * Covers every query helper against a real DB:
 *  - [AuthRoleExclusionDao.searchByRoleIds]: empty-input early return, single-side match,
 *    both-sides-in-set deduplication;
 *  - [AuthRoleExclusionDao.searchByRoleIdAndTenant]: tenant scoping and either-side match;
 *  - [AuthRoleExclusionDao.pairExistsForTenant]: hit, miss (wrong tenant), miss (non-canonical order);
 *  - [AuthRoleExclusionDao.deleteByRoleId]: removes rows that reference the role on either side.
 *
 * Also exercises the Ktorm entity reload path (the PO's generated accessors) via DB reads.
 *
 * Test data source: `AuthRoleExclusionDaoTest.sql`
 *  Tenant t1 has pairs (rA,rB), (rA,rC); tenant t2 has (rA,rB) — same canonical ids, different tenant.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleExclusionDaoTest : RdbTestBase() {

    @Resource
    private lateinit var dao: AuthRoleExclusionDao

    private val tenant1 = "excl-tenant-1-9f2c"
    private val tenant2 = "excl-tenant-2-9f2c"

    private val roleA = "9f2c0000-0000-0000-0000-0000000000a0"
    private val roleB = "9f2c0000-0000-0000-0000-0000000000b0"
    private val roleC = "9f2c0000-0000-0000-0000-0000000000c0"
    private val roleZ = "9f2c0000-0000-0000-0000-0000000000z0" // referenced by nothing

    private val exAB1 = "9f2c0000-0000-0000-0000-0000000000e1" // (rA,rB) tenant1
    private val exAC1 = "9f2c0000-0000-0000-0000-0000000000e2" // (rA,rC) tenant1
    private val exAB2 = "9f2c0000-0000-0000-0000-0000000000e3" // (rA,rB) tenant2

    @Test
    fun searchByRoleIds_empty_returnsEmpty() {
        assertTrue(dao.searchByRoleIds(emptyList()).isEmpty())
    }

    @Test
    fun searchByRoleIds_singleSide_findsInvolvingRows() {
        // roleC only appears as roleBId of exAC1.
        val rows = dao.searchByRoleIds(listOf(roleC))
        assertEquals(setOf(exAC1), rows.map { it.id }.toSet())
    }

    @Test
    fun searchByRoleIds_bothSidesInSet_deduplicates() {
        // roleA and roleB are both in the set; exAB1 has A on one side and B on the other → it must
        // appear exactly once (the UNION-with-dedup contract), and exAC1 (A↔C) also matches via A.
        val rows = dao.searchByRoleIds(listOf(roleA, roleB))
        val ids = rows.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "no duplicate rows")
        assertTrue(ids.toSet().containsAll(setOf(exAB1, exAC1)))
    }

    @Test
    fun searchByRoleIdAndTenant_scopesToTenantAndEitherSide() {
        // tenant1: roleA appears in exAB1 and exAC1; the same canonical pair in tenant2 (exAB2) is excluded.
        val rows = dao.searchByRoleIdAndTenant(roleA, tenant1)
        assertEquals(setOf(exAB1, exAC1), rows.map { it.id }.toSet())
        // roleB on the B side in tenant1 → only exAB1.
        assertEquals(setOf(exAB1), dao.searchByRoleIdAndTenant(roleB, tenant1).map { it.id }.toSet())
    }

    @Test
    fun pairExistsForTenant_hit() {
        assertTrue(dao.pairExistsForTenant(roleA, roleB, tenant1))
    }

    @Test
    fun pairExistsForTenant_wrongTenant_miss() {
        assertFalse(dao.pairExistsForTenant(roleA, roleC, tenant2), "(A,C) only exists in tenant1")
    }

    @Test
    fun pairExistsForTenant_nonCanonicalOrder_miss() {
        // Rows are stored canonically (a<b); querying with the args swapped must not match.
        assertFalse(dao.pairExistsForTenant(roleB, roleA, tenant1))
    }

    @Test
    fun deleteByRoleId_removesEitherSideReferences() {
        // roleA is on the A side of exAB1 and exAC1 (tenant1) and exAB2 (tenant2) → all three go.
        val deleted = dao.deleteByRoleId(roleA)
        assertEquals(3, deleted)
        assertTrue(dao.searchByRoleIds(listOf(roleA)).isEmpty())
        // roleB no longer has the (rA,rB) rows either.
        assertTrue(dao.searchByRoleIds(listOf(roleB)).isEmpty())
    }

    @Test
    fun deleteByRoleId_unreferencedRole_deletesNothing() {
        assertEquals(0, dao.deleteByRoleId(roleZ))
    }
}
