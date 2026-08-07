package io.kudos.ms.auth.core.platform.authz.spi

import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for the generic, fail-closed policy extension seam.
 *
 * @author K
 * @author AI: Codex
 */
internal class AuthorizationPolicyExtensionEvaluatorTest {

    private val request = AuthzRequest(
        subject = SubjectRef.ofUser("u1"),
        permissionCode = "order:refund",
        resourceType = "order",
        instanceId = "o1",
    )

    @Test
    fun unsupportedAndAbstainingContributorsDoNotAffectTheDecision() {
        val unsupported = object : IAuthorizationPolicyContributor {
            override fun supports(request: AuthzRequest): Boolean = false
            override fun evaluate(request: AuthzRequest): AuthorizationPolicyContribution =
                error("must not be called")
        }
        val abstaining = IAuthorizationPolicyContributor { AuthorizationPolicyContribution.abstain() }

        assertTrue(AuthorizationPolicyExtensionEvaluator(listOf(unsupported, abstaining)).evaluate(request).isEmpty())
    }

    @Test
    fun contributionsRemainExplicitForTheDecisionAlgebraToMerge() {
        val allow = IAuthorizationPolicyContributor {
            AuthorizationPolicyContribution.allow("resource-owner")
        }
        val deny = IAuthorizationPolicyContributor {
            AuthorizationPolicyContribution.deny("risk-block", "risk threshold exceeded")
        }

        val results = AuthorizationPolicyExtensionEvaluator(listOf(allow, deny)).evaluate(request)
        assertEquals(
            listOf(AuthorizationPolicyContribution.Effect.ALLOW, AuthorizationPolicyContribution.Effect.DENY),
            results.map { it.effect },
        )
    }

    @Test
    fun contributorFailureBecomesAnExplicitDeny() {
        val broken = IAuthorizationPolicyContributor { error("backend offline") }

        val result = AuthorizationPolicyExtensionEvaluator(listOf(broken)).evaluate(request).single()
        assertEquals(AuthorizationPolicyContribution.Effect.DENY, result.effect)
        assertEquals("extension-unavailable", result.policyCode)
    }
}
