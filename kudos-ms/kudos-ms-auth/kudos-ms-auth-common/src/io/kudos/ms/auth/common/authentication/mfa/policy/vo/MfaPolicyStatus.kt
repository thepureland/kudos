package io.kudos.ms.auth.common.authentication.mfa.policy.vo

import java.time.LocalDateTime

/** Secret-free MFA policy decision for the current account. */
data class MfaPolicyStatus(
    val mode: String,
    val required: Boolean,
    val enrolled: Boolean,
    val enrollmentRequired: Boolean,
    val gracePeriodActive: Boolean,
    val graceExpiresAt: LocalDateTime?,
    /** Set while an administrator-granted enrollment exemption is in force; the user still has to enrol. */
    val exemptionExpiresAt: LocalDateTime? = null,
    val allowedMethods: Set<String>,
    val recoveryCodesEnabled: Boolean,
)
