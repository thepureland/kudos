package io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice

import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicySaveCommand

interface ITenantMfaPolicyService {
    fun getEffective(tenantId: String): EffectiveTenantMfaPolicy
    fun save(command: TenantMfaPolicySaveCommand): EffectiveTenantMfaPolicy
    fun evaluate(tenantId: String, userId: String): MfaPolicyDecision
}
