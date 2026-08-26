package io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.impl

import io.kudos.ms.auth.core.authentication.mfa.policy.TenantMfaPolicyProperties
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.dao.AuthMfaEnrollmentExemptionDao
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionGrantCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionRevokeCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionStatusEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.po.AuthMfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Grants and revokes the per-user pass on MFA enrollment.
 *
 * Three refusals are deliberate rather than incidental:
 *
 * - **An enrolled user cannot be exempted.** Their block is "present your second factor", and lifting that by
 *   administrative action is a way around a credential they still hold. Only the "you have not enrolled yet"
 *   block is in scope.
 * - **Nobody exempts themselves.** An administrator who can both grant and use an exemption has removed MFA
 *   from their own account with one call, which is the escalation this feature would otherwise create.
 * - **The window is bounded and always in the future.** An exemption that never expires is the tenant policy
 *   quietly turned off for that account, with nothing to make anyone look at it again.
 */
@Service
@Transactional
open class MfaEnrollmentExemptionService(
    private val dao: AuthMfaEnrollmentExemptionDao,
    private val policyService: ITenantMfaPolicyService,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val properties: TenantMfaPolicyProperties = TenantMfaPolicyProperties(),
) : IMfaEnrollmentExemptionService {

    @Transactional(readOnly = true)
    override fun activeExemptionExpiry(tenantId: String, userId: String): LocalDateTime? {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        return dao.findActive(tenantId, userId, LocalDateTime.now(clock)).maxOfOrNull { it.expiresAt }
    }

    @Transactional(readOnly = true)
    override fun listRecent(tenantId: String, userId: String?, limit: Int): List<MfaEnrollmentExemption> {
        requireIdentifier(tenantId, TENANT_INVALID)
        userId?.let { requireIdentifier(it, USER_INVALID) }
        if (limit !in 1..MAX_QUERY_LIMIT) fail(LIMIT_INVALID)
        return dao.findRecent(tenantId, userId, limit).map { it.toExemption() }
    }

    override fun grant(command: MfaEnrollmentExemptionGrantCommand): MfaEnrollmentExemption {
        requireIdentifier(command.tenantId, TENANT_INVALID)
        requireIdentifier(command.userId, USER_INVALID)
        requireIdentifier(command.actorUserId, ACTOR_INVALID)
        val reason = command.reason.trim()
        if (reason.isBlank() || reason.length > MAX_REASON_LENGTH || reason.any(Char::isISOControl)) {
            fail(REASON_INVALID)
        }
        if (command.actorUserId == command.userId) fail(SELF_GRANT_FORBIDDEN)
        val now = LocalDateTime.now(clock)
        val maxExpiry = now.plusDays(maxExemptionDays().toLong())
        if (!command.expiresAt.isAfter(now) || command.expiresAt.isAfter(maxExpiry)) fail(WINDOW_INVALID)
        // Tenant membership, account existence and current enrollment all come from the policy decision, which
        // is the same evaluation the login path will make — so an exemption cannot be granted for a state the
        // enforcement point does not actually recognise.
        val decision = policyService.evaluate(command.tenantId, command.userId)
        if (decision.enrolled) fail(ALREADY_ENROLLED)
        if (!decision.required) fail(NOT_REQUIRED)
        val exemption = AuthMfaEnrollmentExemption {
            id = UUID.randomUUID().toString()
            tenantId = command.tenantId
            userId = command.userId
            status = MfaEnrollmentExemptionStatusEnum.ACTIVE.name
            this.reason = reason
            grantedBy = command.actorUserId
            grantedAt = now
            expiresAt = command.expiresAt
            revokedBy = null
            revokeReason = null
            revokedAt = null
        }
        dao.insert(exemption)
        return exemption.toExemption()
    }

    override fun revoke(command: MfaEnrollmentExemptionRevokeCommand): Int {
        requireIdentifier(command.tenantId, TENANT_INVALID)
        requireIdentifier(command.userId, USER_INVALID)
        requireIdentifier(command.actorUserId, ACTOR_INVALID)
        val reason = command.reason.trim()
        if (reason.isBlank() || reason.length > MAX_REASON_LENGTH || reason.any(Char::isISOControl)) {
            fail(REASON_INVALID)
        }
        return dao.revokeActive(
            tenantId = command.tenantId,
            userId = command.userId,
            actorUserId = command.actorUserId,
            reason = reason,
            revokedAt = LocalDateTime.now(clock),
        )
    }

    /** The configured ceiling, itself clamped: a deployment cannot configure an unbounded rescue. */
    private fun maxExemptionDays(): Int =
        properties.maxEnrollmentExemptionDays.coerceIn(MIN_EXEMPTION_DAYS, HARD_MAX_EXEMPTION_DAYS)

    private fun AuthMfaEnrollmentExemption.toExemption() = MfaEnrollmentExemption(
        id = id,
        tenantId = tenantId,
        userId = userId,
        status = runCatching { MfaEnrollmentExemptionStatusEnum.valueOf(status) }
            .getOrElse { fail(STATUS_INVALID) },
        reason = reason,
        grantedBy = grantedBy,
        grantedAt = grantedAt,
        expiresAt = expiresAt,
        revokedBy = revokedBy,
        revokeReason = revokeReason,
        revokedAt = revokedAt,
    )

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > MAX_IDENTIFIER_LENGTH || value.any(Char::isISOControl)) {
            fail(errorCode)
        }
    }

    private fun fail(errorCode: String): Nothing = throw TenantMfaPolicyException(errorCode)

    internal companion object {
        const val MIN_EXEMPTION_DAYS = 1
        const val HARD_MAX_EXEMPTION_DAYS = 30
        const val MAX_QUERY_LIMIT = 200
        const val MAX_REASON_LENGTH = 512
        const val MAX_IDENTIFIER_LENGTH = 36
        const val TENANT_INVALID = "MFA_EXEMPTION_TENANT_INVALID"
        const val USER_INVALID = "MFA_EXEMPTION_USER_INVALID"
        const val ACTOR_INVALID = "MFA_EXEMPTION_ACTOR_INVALID"
        const val REASON_INVALID = "MFA_EXEMPTION_REASON_INVALID"
        const val LIMIT_INVALID = "MFA_EXEMPTION_LIMIT_INVALID"
        const val WINDOW_INVALID = "MFA_EXEMPTION_WINDOW_INVALID"
        const val SELF_GRANT_FORBIDDEN = "MFA_EXEMPTION_SELF_GRANT_FORBIDDEN"
        const val ALREADY_ENROLLED = "MFA_EXEMPTION_ALREADY_ENROLLED"
        const val NOT_REQUIRED = "MFA_EXEMPTION_NOT_REQUIRED"
        const val STATUS_INVALID = "MFA_EXEMPTION_STATUS_INVALID"
    }
}
