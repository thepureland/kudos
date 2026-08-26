package io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice

import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionGrantCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionRevokeCommand
import java.time.LocalDateTime

/** Administrator-controlled rescue for accounts blocked by the MFA enrollment requirement. */
interface IMfaEnrollmentExemptionService {

    /**
     * When the user's enrollment requirement is currently lifted, or `null` when it is not.
     *
     * On the login path, so it answers with the latest active expiry rather than the whole history.
     */
    fun activeExemptionExpiry(tenantId: String, userId: String): LocalDateTime?

    fun listRecent(tenantId: String, userId: String?, limit: Int): List<MfaEnrollmentExemption>

    fun grant(command: MfaEnrollmentExemptionGrantCommand): MfaEnrollmentExemption

    /** Revokes every active grant of the user; returns how many were in force. */
    fun revoke(command: MfaEnrollmentExemptionRevokeCommand): Int
}
