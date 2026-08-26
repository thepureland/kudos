package io.kudos.ms.auth.core.authentication.credentialrevocation.model

import java.time.LocalDateTime

enum class AuthCredentialTypeEnum {
    WEBAUTHN,
    TOTP,
}

/**
 * An administrator taking one credential away from an account.
 *
 * [credentialRef] is the audit row id the administrator already sees, not a raw credential id — the audit view
 * never exposes one, and revocation gives no reason to start.
 */
data class AuthCredentialRevocationCommand(
    val tenantId: String,
    val userId: String,
    val credentialType: AuthCredentialTypeEnum,
    val credentialRef: String? = null,
    val actorUserId: String,
    val reason: String,
    val securityEventId: String? = null,
)

/**
 * What the revocation did, including what it left behind.
 *
 * [leftWithoutFactor] and [enrollmentBlocked] exist so the operator learns immediately that they have just cut
 * the account's last factor and, if the tenant's grace period has already run out, that the user cannot get
 * back in without an enrollment exemption. Revocation is still carried out: a compromised credential has to go,
 * and refusing would leave it usable.
 */
data class AuthCredentialRevocationResult(
    val id: String,
    val tenantId: String,
    val userId: String,
    val credentialType: AuthCredentialTypeEnum,
    val credentialRef: String?,
    val credentialFingerprint: String?,
    val actorUserId: String,
    val reason: String,
    val securityEventId: String?,
    val leftWithoutFactor: Boolean,
    val enrollmentBlocked: Boolean,
    val revokedAt: LocalDateTime,
)

class AuthCredentialRevocationException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
