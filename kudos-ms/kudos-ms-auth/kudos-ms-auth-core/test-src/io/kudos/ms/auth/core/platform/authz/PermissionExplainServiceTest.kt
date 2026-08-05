package io.kudos.ms.auth.core.platform.authz

import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * junit test for [PermissionExplainService].
 *
 * Test data source: `PermissionExplainServiceTest.sql` — the same shape as the decision test, so an
 * explanation can be checked against a verdict that is already pinned elsewhere.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class PermissionExplainServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var explainService: PermissionExplainService

    private val staff = "exp10000-0000-0000-0000-000000000001"
    private val viaGroupUser = "exp10000-0000-0000-0000-000000000002"
    private val nobody = "exp10000-0000-0000-0000-000000000003"

    @Test
    fun explainsAPermitByNamingTheRuleAndTheRole() {
        val explanation = explainService.explain(staff, "sys:user:read")
        assertTrue(explanation.decision.permitted)
        assertEquals(AuthzDecision.Reason.ALLOWED_BY_GRANT, explanation.decision.reason)

        val evidence = explanation.relevantGrants.single { it.effect == PermissionEffect.ALLOW }
        assertEquals("sys:user:*", evidence.permissionCode, "the wildcard is shown as written, not expanded")
        assertEquals("svc-exp-staff-Zt8", evidence.roleCode, "the role is named, not just its id")
        assertEquals("DIRECT", evidence.source)
    }

    @Test
    fun explainsADenialByNamingTheRuleThatCausedIt() {
        val explanation = explainService.explain(staff, "sys:user:delete")
        assertFalse(explanation.decision.permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_RULE, explanation.decision.reason)

        // Both the wide ALLOW and the narrow DENY are relevant: the answer only makes sense if the
        // reader can see that one overrode the other.
        assertEquals(2, explanation.relevantGrants.size)
        assertTrue(explanation.relevantGrants.any { it.effect == PermissionEffect.DENY })
        assertTrue(explanation.relevantGrants.any { it.effect == PermissionEffect.ALLOW })
    }

    @Test
    fun explainsTheMostCommonDenialOfAll_nothingSpeaksToIt() {
        // "Nothing you hold mentions this permission" is the answer operators most need and least
        // often get; an empty evidence list is the explanation, not a missing one.
        val explanation = explainService.explain(nobody, "sys:user:read")
        assertFalse(explanation.decision.permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_DEFAULT, explanation.decision.reason)
        assertTrue(explanation.relevantGrants.isEmpty())
    }

    @Test
    fun namesTheGroupPathWhenTheRoleComesThroughOne() {
        val explanation = explainService.explain(viaGroupUser, "msg:template:read")
        assertTrue(explanation.decision.permitted)
        assertEquals("GROUP", explanation.relevantGrants.single().source)
    }

    @Test
    fun namesTheInheritedPathWhenTheRoleComesFromTheParentChain() {
        // The case operators find most surprising — the permission is on a role they never see
        // assigned to anybody, because it is an ancestor of one that is.
        val explanation = explainService.explain(staff, "ams:base:view")
        assertTrue(explanation.decision.permitted)
        assertEquals("INHERITED", explanation.relevantGrants.single().source)
    }

    @Test
    fun conditionsAreShownAndEvaluatedWithTheSuppliedContext() {
        val withoutContext = explainService.explain(staff, "ops:console:enter")
        assertFalse(withoutContext.decision.permitted, "no context cannot satisfy an ip condition")
        assertEquals("ip=10.0.0.0/8", withoutContext.relevantGrants.single().condition)

        val asSeenFromTheOffice = explainService.explain(staff, "ops:console:enter", mapOf("ip" to "10.1.2.3"))
        assertTrue(asSeenFromTheOffice.decision.permitted, "the same question answered as the real caller sees it")
    }
}
