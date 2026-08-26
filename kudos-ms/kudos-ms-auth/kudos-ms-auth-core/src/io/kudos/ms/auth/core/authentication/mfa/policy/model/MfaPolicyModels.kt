package io.kudos.ms.auth.core.authentication.mfa.policy.model

import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaRequirementModeEnum
import java.time.LocalDateTime

data class TenantMfaPolicySaveCommand(
    val tenantId: String,
    val mode: String,
    val gracePeriodDays: Int,
    val allowedMethods: Set<String>,
    val recoveryCodesEnabled: Boolean,
    val requiredAccountTypeCodes: Set<String>,
    val requiredRoleCodes: Set<String>,
    val actorUserId: String,
    val operationReason: String,
)

data class EffectiveTenantMfaPolicy(
    val tenantId: String,
    val mode: MfaRequirementModeEnum = MfaRequirementModeEnum.OPTIONAL,
    val gracePeriodDays: Int = 7,
    val allowedMethods: Set<MfaMethodEnum> = setOf(MfaMethodEnum.TOTP),
    val recoveryCodesEnabled: Boolean = true,
    val requiredAccountTypeCodes: Set<String> = emptySet(),
    val requiredRoleCodes: Set<String> = emptySet(),
    val effectiveFrom: LocalDateTime? = null,
    val configured: Boolean = false,
)

data class MfaPolicyDecision(
    val policy: EffectiveTenantMfaPolicy,
    val required: Boolean,
    val enrolled: Boolean,
    val enrollmentRequired: Boolean,
    val gracePeriodActive: Boolean,
    val graceExpiresAt: LocalDateTime?,
)

class TenantMfaPolicyException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
