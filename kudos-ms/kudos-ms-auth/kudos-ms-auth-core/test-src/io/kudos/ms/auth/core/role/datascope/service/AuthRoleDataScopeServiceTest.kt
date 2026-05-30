package io.kudos.ms.auth.core.role.datascope.service

import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleDataScopeService.
 *
 * Test data source: `AuthRoleDataScopeServiceTest.sql`
 * Org tree: root(d0) <- child(d1) <- grand(d2); plus standalone other(d3).
 * Every test user belongs to child(d1) except the no-org user.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleDataScopeServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthRoleDataScopeService

    private val orgChild = "5a7a5c0e-0000-0000-0000-0000000000d1"
    private val orgGrand = "5a7a5c0e-0000-0000-0000-0000000000d2"
    private val orgOther = "5a7a5c0e-0000-0000-0000-0000000000d3"

    private val bindTestRoleId = "5a7a5c0e-0000-0000-0000-0000000000f9"

    private val userAll = "5a7a5c0e-0000-0000-0000-0000000000e0"
    private val userOrgChild = "5a7a5c0e-0000-0000-0000-0000000000e1"
    private val userOrg = "5a7a5c0e-0000-0000-0000-0000000000e2"
    private val userSelf = "5a7a5c0e-0000-0000-0000-0000000000e3"
    private val userCustom = "5a7a5c0e-0000-0000-0000-0000000000e4"
    private val userNoOrg = "5a7a5c0e-0000-0000-0000-0000000000e5"

    // ---- Custom org grant management (DAO-backed, no cache) ----

    @Test
    fun bindOrgs_setThenGet() {
        val n = service.bindOrgs(bindTestRoleId, listOf(orgChild, orgGrand))
        assertEquals(2, n)
        assertEquals(setOf(orgChild, orgGrand), service.getOrgIdsByRoleId(bindTestRoleId))
    }

    @Test
    fun bindOrgs_replaceSemantics() {
        service.bindOrgs(bindTestRoleId, listOf(orgChild, orgGrand))
        // Re-bind a different set; the previous set must be fully replaced, not merged.
        val n = service.bindOrgs(bindTestRoleId, listOf(orgOther))
        assertEquals(1, n)
        assertEquals(setOf(orgOther), service.getOrgIdsByRoleId(bindTestRoleId))
    }

    @Test
    fun bindOrgs_emptyClears() {
        service.bindOrgs(bindTestRoleId, listOf(orgChild))
        val n = service.bindOrgs(bindTestRoleId, emptyList())
        assertEquals(0, n)
        assertTrue(service.getOrgIdsByRoleId(bindTestRoleId).isEmpty())
    }

    @Test
    fun bindOrgs_deduplicates() {
        val n = service.bindOrgs(bindTestRoleId, listOf(orgChild, orgChild, orgGrand))
        assertEquals(2, n)
        assertEquals(setOf(orgChild, orgGrand), service.getOrgIdsByRoleId(bindTestRoleId))
    }

    // ---- Update a role's scope code ----

    @Test
    fun updateScope_validCode_succeeds() {
        assertTrue(service.updateScope(bindTestRoleId, "ORG"))
    }

    @Test
    fun updateScope_nullMeansAll_succeeds() {
        assertTrue(service.updateScope(bindTestRoleId, null))
    }

    @Test
    fun updateScope_unknownCode_rejected() {
        assertFailsWith<IllegalArgumentException> { service.updateScope(bindTestRoleId, "BOGUS_SCOPE") }
    }

    // ---- Effective data-scope resolution (most permissive across roles) ----

    @Test
    fun resolve_allScope_isUnrestricted() {
        val vo = service.resolveUserDataScope(userAll)
        assertTrue(vo.all)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun resolve_orgAndChildScope_includesOrgSubtree() {
        val vo = service.resolveUserDataScope(userOrgChild)
        assertFalse(vo.all)
        assertFalse(vo.self)
        // child(d1) plus its descendant grand(d2).
        assertEquals(setOf(orgChild, orgGrand), vo.orgIds)
    }

    @Test
    fun resolve_orgScope_isOwnOrgOnly() {
        val vo = service.resolveUserDataScope(userOrg)
        assertFalse(vo.all)
        assertFalse(vo.self)
        assertEquals(setOf(orgChild), vo.orgIds)
    }

    @Test
    fun resolve_selfScope_setsSelfFlagNoOrgs() {
        val vo = service.resolveUserDataScope(userSelf)
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun resolve_customScope_usesExplicitOrgGrants() {
        val vo = service.resolveUserDataScope(userCustom)
        assertFalse(vo.all)
        assertFalse(vo.self)
        assertEquals(setOf(orgOther), vo.orgIds)
    }

    @Test
    fun resolve_orgScopeButNoOrg_fallsBackToSelfOnly() {
        // userNoOrg holds an ORG-scoped role but has no org → nothing concrete resolves.
        val vo = service.resolveUserDataScope(userNoOrg)
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun resolve_userWithoutRoles_isSelfOnly() {
        val vo = service.resolveUserDataScope("non-existent-user-id")
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }
}
