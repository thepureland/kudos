package io.kudos.ms.auth.core.authentication.credential.service.impl

import io.kudos.ms.auth.core.authentication.credential.dao.AuthCredentialDao
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialException
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSecretTypeEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStatusEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStoreCommand
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSummary
import io.kudos.ms.auth.core.authentication.credential.model.po.AuthCredential
import io.kudos.ms.auth.core.authentication.credential.service.iservice.IAuthCredentialService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Holds the auth domain's password, security-password and TOTP secrets.
 *
 * **The secret never leaves, and there is no getter for it.** Every read here returns [AuthCredentialSummary],
 * which has no secret field; the operations that genuinely need the stored value — [verifyWith], [rotateWith]
 * and [matches] — take it inward as a lambda argument. That is deliberate: a service that can hand back an
 * encoded password is one careless log statement or one added response field away from leaking it, and the
 * compiler will not warn about either.
 */
@Service
@Transactional
open class AuthCredentialService(
    private val dao: AuthCredentialDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : IAuthCredentialService {

    override fun store(command: AuthCredentialStoreCommand): AuthCredentialSummary {
        requireIdentifier(command.tenantId, TENANT_INVALID)
        requireIdentifier(command.userId, USER_INVALID)
        val secret = command.secretHashOrRef.trim()
        if (secret.isEmpty() || secret.length > MAX_SECRET_LENGTH) fail(SECRET_INVALID)
        val metadata = command.metadata?.trim()?.takeIf { it.isNotEmpty() }
        if (metadata != null && metadata.length > MAX_METADATA_LENGTH) fail(METADATA_INVALID)
        val now = LocalDateTime.now(clock)
        if (command.expiresAt != null && !command.expiresAt.isAfter(now)) fail(EXPIRY_INVALID)
        val existing = dao.findActive(command.tenantId, command.userId, command.type.name)
        if (existing == null) {
            if (command.expectedVersion != null) fail(VERSION_CONFLICT)
            val credential = AuthCredential {
                id = UUID.randomUUID().toString()
                tenantId = command.tenantId
                userId = command.userId
                type = command.type.name
                secretHashOrRef = secret
                status = AuthCredentialStatusEnum.ACTIVE.name
                version = 0
                enrolledAt = now
                expiresAt = command.expiresAt
                lastUsedAt = null
                this.metadata = metadata
                revokedAt = null
                revokeReason = null
                createTime = now
                updateTime = now
            }
            dao.insert(credential)
            return credential.toSummary()
        }
        val expectedVersion = command.expectedVersion ?: fail(VERSION_REQUIRED)
        if (!dao.rotate(existing.id, expectedVersion, secret, command.expiresAt, metadata, now)) {
            fail(VERSION_CONFLICT)
        }
        return requireNotNull(dao.findActive(command.tenantId, command.userId, command.type.name)).toSummary()
    }

    @Transactional(readOnly = true)
    override fun findActive(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
    ): AuthCredentialSummary? {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        return dao.findActive(tenantId, userId, type.name)?.takeIf { it.isUsable() }?.toSummary()
    }

    @Transactional(readOnly = true)
    override fun listForUser(tenantId: String, userId: String): List<AuthCredentialSummary> {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        return dao.findByUser(tenantId, userId).map { it.toSummary() }
    }

    @Transactional(readOnly = true)
    override fun matches(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        candidateHashOrRef: String,
    ): Boolean = verifyWith(tenantId, userId, type) { constantTimeEquals(it, candidateHashOrRef) }

    @Transactional(readOnly = true)
    override fun verifyWith(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        verifier: (storedSecret: String) -> Boolean,
    ): Boolean {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        val stored = dao.findActive(tenantId, userId, type.name)?.takeIf { it.isUsable() } ?: return false
        return verifier(stored.secretHashOrRef)
    }

    override fun rotateWith(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        rotator: (storedSecret: String) -> String?,
    ): Boolean {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        val current = dao.findActive(tenantId, userId, type.name)?.takeIf { it.isUsable() } ?: return false
        val replacement = rotator(current.secretHashOrRef)?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        if (replacement.length > MAX_SECRET_LENGTH) fail(SECRET_INVALID)
        // Under the version read a moment ago, so a password change that landed in between wins and this
        // rotation is simply dropped rather than reinstating the secret that change replaced.
        return dao.rotate(
            current.id,
            current.version,
            replacement,
            current.expiresAt,
            current.metadata,
            LocalDateTime.now(clock),
        )
    }

    override fun recordUsage(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
    ): Boolean {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        val credential = dao.findActive(tenantId, userId, type.name) ?: return false
        return dao.recordUsage(credential.id, LocalDateTime.now(clock))
    }

    override fun revoke(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        reason: String,
    ): Int {
        requireIdentifier(tenantId, TENANT_INVALID)
        requireIdentifier(userId, USER_INVALID)
        val trimmed = reason.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_REASON_LENGTH || trimmed.any(Char::isISOControl)) {
            fail(REASON_INVALID)
        }
        return dao.revoke(tenantId, userId, type.name, trimmed, LocalDateTime.now(clock))
    }

    /** An expired credential is still stored, but it is not one anybody can authenticate with. */
    private fun AuthCredential.isUsable(): Boolean =
        revokedAt == null && (expiresAt == null || LocalDateTime.now(clock).isBefore(expiresAt))

    /**
     * Compares digests of both sides rather than the values themselves.
     *
     * Encoded secrets are not all self-comparing — a stored `{bcrypt}` hash and a candidate hash are equal
     * strings only when the caller encoded with the same salt, which it does for the reference-style values
     * this method is for. Digesting first makes the comparison take the same time whatever the inputs, so a
     * caller cannot learn where two values start to differ by timing this.
     */
    private fun constantTimeEquals(stored: String, candidate: String): Boolean =
        MessageDigest.isEqual(sha256(stored), sha256(candidate))

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    private fun AuthCredential.toSummary() = AuthCredentialSummary(
        id = id,
        tenantId = tenantId,
        userId = userId,
        type = runCatching { AuthCredentialSecretTypeEnum.valueOf(type) }.getOrElse { fail(TYPE_INVALID) },
        status = runCatching { AuthCredentialStatusEnum.valueOf(status) }.getOrElse { fail(STATUS_INVALID) },
        version = version,
        enrolledAt = enrolledAt,
        expiresAt = expiresAt,
        lastUsedAt = lastUsedAt,
        metadata = metadata,
        revokedAt = revokedAt,
        revokeReason = revokeReason,
    )

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > MAX_IDENTIFIER_LENGTH || value.any(Char::isISOControl)) {
            fail(errorCode)
        }
    }

    private fun fail(errorCode: String): Nothing = throw AuthCredentialException(errorCode)

    internal companion object {
        const val MAX_IDENTIFIER_LENGTH = 36
        const val MAX_SECRET_LENGTH = 1024
        const val MAX_METADATA_LENGTH = 1024
        const val MAX_REASON_LENGTH = 512
        const val TENANT_INVALID = "AUTH_CREDENTIAL_TENANT_INVALID"
        const val USER_INVALID = "AUTH_CREDENTIAL_USER_INVALID"
        const val SECRET_INVALID = "AUTH_CREDENTIAL_SECRET_INVALID"
        const val METADATA_INVALID = "AUTH_CREDENTIAL_METADATA_INVALID"
        const val EXPIRY_INVALID = "AUTH_CREDENTIAL_EXPIRY_INVALID"
        const val REASON_INVALID = "AUTH_CREDENTIAL_REASON_INVALID"
        const val VERSION_REQUIRED = "AUTH_CREDENTIAL_VERSION_REQUIRED"
        const val VERSION_CONFLICT = "AUTH_CREDENTIAL_VERSION_CONFLICT"
        const val TYPE_INVALID = "AUTH_CREDENTIAL_TYPE_INVALID"
        const val STATUS_INVALID = "AUTH_CREDENTIAL_STATUS_INVALID"
    }
}
