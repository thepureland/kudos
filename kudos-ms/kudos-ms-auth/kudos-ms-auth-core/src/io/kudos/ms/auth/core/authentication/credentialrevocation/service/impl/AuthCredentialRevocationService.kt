package io.kudos.ms.auth.core.authentication.credentialrevocation.service.impl

import io.kudos.ms.auth.core.authentication.credentialrevocation.dao.AuthCredentialRevocationDao
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationCommand
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationException
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationResult
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialTypeEnum
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.po.AuthCredentialRevocation
import io.kudos.ms.auth.core.authentication.credentialrevocation.service.iservice.IAuthCredentialRevocationService
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialFingerprints
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Takes a credential away from an account on an administrator's authority.
 *
 * **Why this is never refused for being the last factor.** The self-service unbind path refuses to remove
 * somebody's only way in, and that is right there: the user is choosing, and can choose otherwise. Here the
 * premise is different — the credential is believed compromised, so leaving it usable to protect the user's
 * convenience protects the attacker instead. The revocation goes through and the result reports what the
 * account has left, which is the information an operator needs to arrange the enrollment exemption that gets
 * the user back in.
 *
 * The raw WebAuthn credential id never leaves core: the caller names the audit row it already sees, and this
 * service resolves it internally.
 */
@Service
@Transactional
open class AuthCredentialRevocationService(
    private val dao: AuthCredentialRevocationDao,
    private val credentialDao: AuthWebAuthnCredentialDao,
    private val webAuthnCredentialService: IWebAuthnCredentialService,
    private val totpEnrollmentService: ITotpEnrollmentService,
    private val policyService: ITenantMfaPolicyService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : IAuthCredentialRevocationService {

    override fun revoke(command: AuthCredentialRevocationCommand): AuthCredentialRevocationResult {
        requireIdentifier(command.tenantId, TENANT_INVALID)
        requireIdentifier(command.userId, USER_INVALID)
        requireIdentifier(command.actorUserId, ACTOR_INVALID)
        command.securityEventId?.let { requireIdentifier(it, EVENT_INVALID) }
        val reason = command.reason.trim()
        if (reason.isBlank() || reason.length > MAX_REASON_LENGTH || reason.any(Char::isISOControl)) {
            fail(REASON_INVALID)
        }
        val fingerprint = when (command.credentialType) {
            AuthCredentialTypeEnum.WEBAUTHN -> revokeWebAuthn(command)
            AuthCredentialTypeEnum.TOTP -> {
                if (command.credentialRef != null) fail(CREDENTIAL_REF_UNEXPECTED)
                if (!totpEnrollmentService.disable(command.userId, command.tenantId)) fail(TOTP_NOT_ENROLLED)
                null
            }
        }
        // Evaluated after the credential is gone, so it describes what the account is actually left with
        // rather than what it had a moment ago.
        val decision = policyService.evaluate(command.tenantId, command.userId)
        val now = LocalDateTime.now(clock)
        val record = AuthCredentialRevocation {
            id = UUID.randomUUID().toString()
            tenantId = command.tenantId
            userId = command.userId
            credentialType = command.credentialType.name
            credentialRef = command.credentialRef
            credentialFingerprint = fingerprint
            actorUserId = command.actorUserId
            this.reason = reason
            securityEventId = command.securityEventId
            leftWithoutFactor = !decision.enrolled
            revokedAt = now
        }
        dao.insert(record)
        return record.toResult(
            // Blocked only when the tenant requires a factor the account no longer has and neither the grace
            // period nor an existing exemption would let them back in to enrol a replacement.
            enrollmentBlocked = decision.required && !decision.enrolled && !decision.gracePeriodActive,
        )
    }

    @Transactional(readOnly = true)
    override fun listRecent(
        tenantId: String,
        userId: String?,
        limit: Int,
    ): List<AuthCredentialRevocationResult> {
        requireIdentifier(tenantId, TENANT_INVALID)
        userId?.let { requireIdentifier(it, USER_INVALID) }
        if (limit !in 1..MAX_QUERY_LIMIT) fail(LIMIT_INVALID)
        return dao.findRecent(tenantId, userId, limit).map { it.toResult(enrollmentBlocked = false) }
    }

    /**
     * @return the audit fingerprint of the credential that was revoked
     */
    private fun revokeWebAuthn(command: AuthCredentialRevocationCommand): String {
        val credentialRef = command.credentialRef?.trim()?.takeIf { it.isNotEmpty() } ?: fail(CREDENTIAL_REF_REQUIRED)
        // Scoped by tenant and user before the row is touched, so an id from elsewhere resolves to nothing
        // rather than to somebody else's credential.
        val credential = credentialDao.findByUser(command.tenantId, command.userId)
            .singleOrNull { it.id == credentialRef }
            ?: fail(CREDENTIAL_NOT_FOUND)
        if (credential.revokedAt != null) fail(CREDENTIAL_ALREADY_REVOKED)
        if (!webAuthnCredentialService.revoke(command.tenantId, command.userId, credential.credentialId)) {
            fail(CREDENTIAL_ALREADY_REVOKED)
        }
        return WebAuthnCredentialFingerprints.sha256(credential.credentialId)
    }

    private fun AuthCredentialRevocation.toResult(enrollmentBlocked: Boolean) = AuthCredentialRevocationResult(
        id = id,
        tenantId = tenantId,
        userId = userId,
        credentialType = runCatching { AuthCredentialTypeEnum.valueOf(credentialType) }
            .getOrElse { fail(CREDENTIAL_TYPE_INVALID) },
        credentialRef = credentialRef,
        credentialFingerprint = credentialFingerprint,
        actorUserId = actorUserId,
        reason = reason,
        securityEventId = securityEventId,
        leftWithoutFactor = leftWithoutFactor,
        enrollmentBlocked = enrollmentBlocked,
        revokedAt = revokedAt,
    )

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > MAX_IDENTIFIER_LENGTH || value.any(Char::isISOControl)) {
            fail(errorCode)
        }
    }

    private fun fail(errorCode: String): Nothing = throw AuthCredentialRevocationException(errorCode)

    internal companion object {
        const val MAX_QUERY_LIMIT = 200
        const val MAX_REASON_LENGTH = 512
        const val MAX_IDENTIFIER_LENGTH = 36
        const val TENANT_INVALID = "AUTH_CREDENTIAL_REVOCATION_TENANT_INVALID"
        const val USER_INVALID = "AUTH_CREDENTIAL_REVOCATION_USER_INVALID"
        const val ACTOR_INVALID = "AUTH_CREDENTIAL_REVOCATION_ACTOR_INVALID"
        const val EVENT_INVALID = "AUTH_CREDENTIAL_REVOCATION_EVENT_INVALID"
        const val REASON_INVALID = "AUTH_CREDENTIAL_REVOCATION_REASON_INVALID"
        const val LIMIT_INVALID = "AUTH_CREDENTIAL_REVOCATION_LIMIT_INVALID"
        const val CREDENTIAL_REF_REQUIRED = "AUTH_CREDENTIAL_REVOCATION_REF_REQUIRED"
        const val CREDENTIAL_REF_UNEXPECTED = "AUTH_CREDENTIAL_REVOCATION_REF_UNEXPECTED"
        const val CREDENTIAL_NOT_FOUND = "AUTH_CREDENTIAL_REVOCATION_CREDENTIAL_NOT_FOUND"
        const val CREDENTIAL_ALREADY_REVOKED = "AUTH_CREDENTIAL_REVOCATION_ALREADY_REVOKED"
        const val CREDENTIAL_TYPE_INVALID = "AUTH_CREDENTIAL_REVOCATION_TYPE_INVALID"
        const val TOTP_NOT_ENROLLED = "AUTH_CREDENTIAL_REVOCATION_TOTP_NOT_ENROLLED"
    }
}
