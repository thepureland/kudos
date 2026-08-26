package io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice

import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum

/** Method-neutral enrollment view used by policy enforcement. */
interface IMfaEnrollmentQuery {
    fun enrolledMethods(userId: String, tenantId: String): Set<MfaMethodEnum>
}
