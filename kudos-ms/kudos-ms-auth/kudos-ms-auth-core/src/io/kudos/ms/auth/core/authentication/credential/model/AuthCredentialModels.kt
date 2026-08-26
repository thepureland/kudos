package io.kudos.ms.auth.core.authentication.credential.model

import java.time.LocalDateTime

/**
 * The secret kinds `auth_credential` itself holds.
 *
 * WebAuthn credentials and recovery codes are deliberately absent: each needs columns this table does not have
 * (public keys and signature counters; per-code hashes and one-time consumption), which is why the design
 * gives them their own tables rather than a `metadata` blob here.
 */
enum class AuthCredentialSecretTypeEnum {
    PASSWORD,
    SECURITY_PASSWORD,
    TOTP,
}

enum class AuthCredentialStatusEnum {
    ACTIVE,
    REVOKED,
}

/**
 * A credential without its secret.
 *
 * The secret never appears in this projection: reads exist to answer "does this account have one, since when,
 * when was it last used", and a shape that also carried the hash would make it one refactor away from being
 * logged or serialised into a response.
 */
data class AuthCredentialSummary(
    val id: String,
    val tenantId: String,
    val userId: String,
    val type: AuthCredentialSecretTypeEnum,
    val status: AuthCredentialStatusEnum,
    val version: Long,
    val enrolledAt: LocalDateTime,
    val expiresAt: LocalDateTime?,
    val lastUsedAt: LocalDateTime?,
    val metadata: String?,
    val revokedAt: LocalDateTime?,
    val revokeReason: String?,
)

/**
 * Stores or rotates one credential.
 *
 * [expectedVersion] is `null` for a first enrolment and the current version for a rotation; a rotation whose
 * version no longer matches lost a race with another change and must not overwrite it.
 */
data class AuthCredentialStoreCommand(
    val tenantId: String,
    val userId: String,
    val type: AuthCredentialSecretTypeEnum,
    val secretHashOrRef: String,
    val expectedVersion: Long? = null,
    val expiresAt: LocalDateTime? = null,
    val metadata: String? = null,
)

class AuthCredentialException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
