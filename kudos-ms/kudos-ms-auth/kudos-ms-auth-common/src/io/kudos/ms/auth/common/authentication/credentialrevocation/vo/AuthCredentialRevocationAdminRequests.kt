package io.kudos.ms.auth.common.authentication.credentialrevocation.vo

/**
 * Revokes one credential of a same-tenant account.
 *
 * [credentialRef] is the audit row id from the credential audit view, not a raw credential id; it is required
 * for `WEBAUTHN` and must be absent for `TOTP`, which is singular per account.
 */
data class AuthCredentialRevocationAdminRequest(
    val userId: String,
    val credentialType: String,
    val credentialRef: String? = null,
    val reason: String,
    /** Optional link to the security event that justified this, so the decision keeps its context. */
    val securityEventId: String? = null,
)
