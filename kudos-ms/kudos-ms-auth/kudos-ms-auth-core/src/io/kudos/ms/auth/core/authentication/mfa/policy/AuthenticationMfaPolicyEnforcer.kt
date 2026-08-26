package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.assurance.spi.IAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import org.springframework.stereotype.Component
import java.time.LocalDateTime

enum class MfaPolicyEnforcementOutcomeEnum {
    ALLOW,
    ALLOW_ENROLLMENT_REQUIRED,
    DENY_ENROLLMENT_REQUIRED,
    REQUIRE_SECOND_FACTOR,
}

data class MfaPolicyEnforcementResult(
    val outcome: MfaPolicyEnforcementOutcomeEnum,
    val decision: MfaPolicyDecision? = null,
    /** Set when an administrator's enrollment exemption is what allowed this sign-in through. */
    val exemptionExpiresAt: LocalDateTime? = null,
)

/**
 * Applies one tenant policy decision consistently to local and federated authentication completions.
 *
 * The exemption lives here rather than inside the policy decision on purpose: `evaluate` answers "what does
 * this tenant's policy say about this account", and an administrator's rescue is an override applied where the
 * decision is enforced. Keeping it here also keeps the dependency one-way — the exemption service evaluates
 * policy when granting, so folding it back into the policy service would make the two circular.
 */
@Component
open class AuthenticationMfaPolicyEnforcer(
    private val policyService: ITenantMfaPolicyService,
    private val assurancePolicy: IAuthenticationAssurancePolicy = DefaultAuthenticationAssurancePolicy(),
    private val exemptionService: IMfaEnrollmentExemptionService? = null,
) {

    open fun enforce(
        purpose: AuthenticationTransactionPurposeEnum,
        tenantId: String,
        userId: String,
        achievedAcr: String,
    ): MfaPolicyEnforcementResult {
        if (purpose != AuthenticationTransactionPurposeEnum.LOGIN) {
            return MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.ALLOW)
        }
        val decision = policyService.evaluate(tenantId, userId)
        if (!decision.required || assurancePolicy.isSatisfied(achievedAcr, DefaultAuthenticationAssurancePolicy.ACR_MFA)) {
            return MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.ALLOW, decision)
        }
        // Checked before either pass: an account that holds a factor is asked for it, whatever an administrator
        // granted. An exemption lifts "you must enrol first", never "prove it is you".
        if (decision.enrolled) {
            return MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR, decision)
        }
        if (decision.gracePeriodActive) {
            return MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED, decision)
        }
        // Only looked up once it could change the answer, so the common paths cost no extra query.
        val exemptionExpiresAt = exemptionService?.activeExemptionExpiry(tenantId, userId)
            ?: return MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED, decision)
        return MfaPolicyEnforcementResult(
            MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED,
            decision,
            exemptionExpiresAt,
        )
    }

    open fun recoveryCodesEnabled(tenantId: String): Boolean =
        policyService.getEffective(tenantId).recoveryCodesEnabled
}
