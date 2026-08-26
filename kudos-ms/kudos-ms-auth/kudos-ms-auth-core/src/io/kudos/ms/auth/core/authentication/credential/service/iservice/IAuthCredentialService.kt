package io.kudos.ms.auth.core.authentication.credential.service.iservice

import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSecretTypeEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStoreCommand
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSummary

/**
 * The auth domain's own credential store.
 *
 * Reads never yield the secret. The two operations that need it — verifying a presented secret and
 * re-encoding a stored one — take a lambda instead, so the encoded value is used where it lives and has no
 * reason to travel.
 */
interface IAuthCredentialService {

    /** Enrols or rotates one credential; rotation requires the current version. */
    fun store(command: AuthCredentialStoreCommand): AuthCredentialSummary

    fun findActive(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
    ): AuthCredentialSummary?

    fun listForUser(tenantId: String, userId: String): List<AuthCredentialSummary>

    /**
     * Whether the account's active credential of this kind equals [candidateHashOrRef], compared in constant
     * time.
     *
     * For reference-style secrets — a stored TOTP secret, not a salted password hash — where the caller holds
     * the exact value it expects. A password needs [verifyWith] instead, because two BCrypt encodings of the
     * same password are different strings.
     */
    fun matches(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        candidateHashOrRef: String,
    ): Boolean

    /**
     * Runs [verifier] against the stored secret and answers with its verdict.
     *
     * A lambda rather than a getter, because verifying a BCrypt hash needs the hash and the plaintext in the
     * same place: somebody has to hold both. Passing the check inward keeps the encoded secret inside this
     * service, where a getter would put it in the caller — and then one careless log line or one added
     * response field away from leaving the process.
     *
     * @return false when there is no usable credential of this kind, without invoking [verifier]
     */
    fun verifyWith(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        verifier: (storedSecret: String) -> Boolean,
    ): Boolean

    /**
     * Offers the stored secret to [rotator] and stores what it returns, under the version just read.
     *
     * Returning null means "no rotation needed". The compare-and-set lives here rather than in the caller so
     * that the version never has to travel with the secret; a concurrent change simply makes this return false.
     */
    fun rotateWith(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        rotator: (storedSecret: String) -> String?,
    ): Boolean

    /** Records that the credential was just used; deliberately does not disturb the optimistic version. */
    fun recordUsage(tenantId: String, userId: String, type: AuthCredentialSecretTypeEnum): Boolean

    fun revoke(
        tenantId: String,
        userId: String,
        type: AuthCredentialSecretTypeEnum,
        reason: String,
    ): Int
}
