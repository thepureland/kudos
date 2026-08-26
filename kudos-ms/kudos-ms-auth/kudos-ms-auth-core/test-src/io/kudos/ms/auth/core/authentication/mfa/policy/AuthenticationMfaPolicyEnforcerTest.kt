package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaRequirementModeEnum
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthenticationMfaPolicyEnforcerTest {
    private val policies = mock(ITenantMfaPolicyService::class.java)
    private val exemptions = mock(IMfaEnrollmentExemptionService::class.java)
    private val enforcer = AuthenticationMfaPolicyEnforcer(policies, exemptionService = exemptions)

    @Test
    fun requiredUnenrolledAccountIsAdvisoryOnlyDuringGrace() {
        `when`(policies.evaluate("t-1", "u-1")).thenReturn(decision(enrolled = false, grace = true))

        val result = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
        )

        assertEquals(MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED, result.outcome)
    }

    @Test
    fun requiredUnenrolledAccountIsDeniedAfterGrace() {
        `when`(policies.evaluate("t-1", "u-1")).thenReturn(decision(enrolled = false, grace = false))

        val result = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
        )

        assertEquals(MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED, result.outcome)
    }

    @Test
    fun enrolledAccountRequiresSecondFactorUntilMfaAcrIsReached() {
        `when`(policies.evaluate("t-1", "u-1")).thenReturn(decision(enrolled = true, grace = false))

        val weak = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_FEDERATED,
        )
        val strong = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_MFA,
        )

        assertEquals(MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR, weak.outcome)
        assertEquals(MfaPolicyEnforcementOutcomeEnum.ALLOW, strong.outcome)
    }

    @Test
    fun anAdministratorExemptionLetsAnUnenrolledAccountInWhileStillPromptingIt() {
        `when`(policies.evaluate("t-1", "u-1"))
            .thenReturn(decision(enrolled = false, grace = false))
        `when`(exemptions.activeExemptionExpiry("t-1", "u-1")).thenReturn(EXEMPT_UNTIL)

        val result = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
        )

        assertEquals(MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED, result.outcome)
        assertEquals(EXEMPT_UNTIL, result.exemptionExpiresAt)
    }

    @Test
    fun anExemptionNeverStandsInForAFactorTheAccountStillHolds() {
        // The dangerous case: an exemption exists *and* the account is enrolled. It must still be challenged,
        // or an administrator could take MFA off an account by granting it a rescue it does not need.
        `when`(policies.evaluate("t-1", "u-1"))
            .thenReturn(decision(enrolled = true, grace = false))
        `when`(exemptions.activeExemptionExpiry("t-1", "u-1")).thenReturn(EXEMPT_UNTIL)

        val result = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
        )

        assertEquals(MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR, result.outcome)
    }

    @Test
    fun stepUpDoesNotReapplyLoginEnrollmentPolicy() {
        val result = enforcer.enforce(
            AuthenticationTransactionPurposeEnum.STEP_UP,
            "t-1",
            "u-1",
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
        )

        assertEquals(MfaPolicyEnforcementOutcomeEnum.ALLOW, result.outcome)
        verify(policies, never()).evaluate("t-1", "u-1")
    }

    private fun decision(enrolled: Boolean, grace: Boolean): MfaPolicyDecision {
        val policy = EffectiveTenantMfaPolicy(
            tenantId = "t-1",
            mode = MfaRequirementModeEnum.REQUIRED,
            configured = true,
        )
        return MfaPolicyDecision(
            policy = policy,
            required = true,
            enrolled = enrolled,
            enrollmentRequired = !enrolled,
            gracePeriodActive = grace,
            graceExpiresAt = null,
        )
    }

    private companion object {
        val EXEMPT_UNTIL: LocalDateTime = LocalDateTime.parse("2026-08-28T10:00:00")
    }
}
