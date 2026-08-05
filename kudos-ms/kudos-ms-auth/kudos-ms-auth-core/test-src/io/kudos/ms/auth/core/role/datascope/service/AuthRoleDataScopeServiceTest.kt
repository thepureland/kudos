package io.kudos.ms.auth.core.role.datascope.service

import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    /** The CUSTOM-scoped role held by [userCustom]; grants on it drive that user's resolution. */
    private val roleCustomForUser = "5a7a5c0e-0000-0000-0000-0000000000f4"

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

    // -------------------- the scope model is multi-dimensional --------------------

    @Test
    fun scopeGrantsAreDimensionQualified() {
        // Organization is the built-in dimension, but it is data rather than a table name: an
        // application slicing rows by region should not have to fork the model to get it.
        service.bindOrgs(bindTestRoleId, listOf(orgOther))
        service.bindScope(bindTestRoleId, "region", listOf("emea", "apac"))

        assertEquals(setOf("emea", "apac"), service.getScopeValues(bindTestRoleId, "region"))
        assertEquals(setOf(orgOther), service.getOrgIdsByRoleId(bindTestRoleId), "the org grants are untouched")
        assertEquals(setOf("org", "region"), service.getAllScopes(bindTestRoleId).keys)
    }

    @Test
    fun rebindingOneDimensionLeavesTheOthersAlone() {
        // The failure this prevents: an admin edits a role's orgs on one screen and silently wipes
        // the regions somebody configured on another.
        service.bindScope(bindTestRoleId, "region", listOf("emea"))
        service.bindOrgs(bindTestRoleId, listOf(orgOther))

        assertEquals(setOf("emea"), service.getScopeValues(bindTestRoleId, "region"))
        assertEquals(setOf(orgOther), service.getOrgIdsByRoleId(bindTestRoleId))
    }

    @Test
    fun clearingADimensionRemovesOnlyThatDimension() {
        service.bindOrgs(bindTestRoleId, listOf(orgOther))
        service.bindScope(bindTestRoleId, "region", listOf("emea"))
        assertEquals(0, service.bindScope(bindTestRoleId, "region", emptyList()))
        assertTrue(service.getScopeValues(bindTestRoleId, "region").isEmpty())
        assertEquals(setOf(orgOther), service.getOrgIdsByRoleId(bindTestRoleId))
    }

    @Test
    fun aScopeGrantMustNameItsDimension() {
        assertFailsWith<IllegalArgumentException> { service.bindScope(bindTestRoleId, "  ", listOf("x")) }
    }

    // -------------------- resolution is multi-dimensional too --------------------

    @Test
    fun resolve_customScope_carriesEveryDimensionTheRoleWasGranted() {
        // The gap this closes: storage and registration were dimension-aware, but resolution read
        // only `org`. A second dimension could then be configured and would silently never reach a
        // consumer — worse than not supporting it, because the screen says it is configured.
        service.bindScope(roleCustomForUser, "region", listOf("emea"))

        val vo = service.resolveUserDataScope(userCustom)
        assertFalse(vo.all)
        assertEquals(setOf(orgOther), vo.orgIds, "the org dimension still resolves as before")
        assertEquals(setOf("emea"), vo.valuesOf("region"), "and so does the one the application added")
        assertTrue(vo.restricts("region"))
    }

    @Test
    fun resolve_anUnrestrictedDimensionIsAbsentRatherThanEmpty() {
        // A consumer must be able to tell "this dimension does not restrict" from "restricted to
        // nothing"; conflating them would turn an unconfigured dimension into an impossible
        // predicate that hides every row.
        val vo = service.resolveUserDataScope(userCustom)
        assertTrue(vo.valuesOf("no-such-dimension").isEmpty())
        assertFalse(vo.restricts("no-such-dimension"))
    }

    @Test
    fun resolve_expansionFollowsThePolicyNotTheDimension() {
        // ORG_AND_CHILD carries the subtree; ORG does not. Expansion belongs to the policy, so a
        // resolver that expanded every dimension uniformly would quietly turn the second into the
        // first — which is exactly what an earlier version of this code did.
        val withChildren = service.resolveUserDataScope(userOrgChild)
        assertTrue(withChildren.orgIds.contains(orgChild), "own org")
        assertTrue(withChildren.orgIds.contains(orgGrand), "and its descendants")

        val ownOnly = service.resolveUserDataScope(userOrg)
        assertEquals(setOf(orgChild), ownOnly.orgIds, "ORG must not pick up the subtree")

        // A CUSTOM list is likewise exactly what an admin picked.
        assertEquals(setOf(orgOther), service.resolveUserDataScope(userCustom).orgIds)
    }

    @Test
    fun resolve_allShortCircuitsToNoDimensions() {
        val vo = service.resolveUserDataScope(userAll)
        assertTrue(vo.all)
        assertTrue(vo.dimensions.isEmpty(), "ALL means unrestricted, not restricted to everything")
    }

    // -------------------- explaining the outcome --------------------

    @Test
    fun explain_namesTheRoleWhoseAllOverridesEveryRestriction() {
        // The question this endpoint exists for. userAll holds an ALL-scoped role, so every other
        // restriction on them is irrelevant — and reading one role's configuration would never
        // reveal that.
        val explanation = service.explainUserDataScope(userAll)
        assertTrue(explanation.resolved.all)
        assertNotNull(explanation.overriddenBy, "an ALL role must be named, not merely implied")
        assertEquals("ALL", explanation.overriddenBy!!.policy)
    }

    @Test
    fun explain_breaksTheOutcomeDownPerRole() {
        val explanation = service.explainUserDataScope(userCustom)
        assertFalse(explanation.resolved.all)
        assertNull(explanation.overriddenBy, "nothing overrode anything here")

        val custom = explanation.contributions.single { it.policy == "CUSTOM" }
        assertEquals(setOf(orgOther), custom.contributed["org"], "a role's own contribution, not the merged set")
        assertEquals("DIRECT", custom.source)
        assertNotNull(custom.roleName, "the role is named so the reader can go and change it")
    }

    @Test
    fun explain_showsWhatEachPolicyContributesOnItsOwn() {
        // ORG contributes only the subject's own org; ORG_AND_CHILD contributes the subtree. Showing
        // the merged result for both would hide exactly the difference the reader is looking for.
        val ownOrgOnly = service.explainUserDataScope(userOrg).contributions.single { it.policy == "ORG" }
        assertEquals(setOf(orgChild), ownOrgOnly.contributed["org"])

        val withChildren = service.explainUserDataScope(userOrgChild)
            .contributions.single { it.policy == "ORG_AND_CHILD" }
        assertTrue(withChildren.contributed["org"]!!.containsAll(setOf(orgChild, orgGrand)))
    }

    @Test
    fun explain_marksTheSelfContribution() {
        val explanation = service.explainUserDataScope(userSelf)
        assertTrue(explanation.contributions.any { it.contributesSelf })
        assertTrue(explanation.resolved.self)
    }

    @Test
    fun explain_forASubjectWithNoRolesIsEmptyButValid() {
        val explanation = service.explainUserDataScope("non-existent-user-id")
        assertTrue(explanation.contributions.isEmpty())
        assertTrue(explanation.resolved.self, "the restrictive fallback still applies")
    }
}
