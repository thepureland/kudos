package io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One verified WebAuthn public-key credential. Private key material never reaches the server. */
interface AuthWebAuthnCredential : IDbEntity<String, AuthWebAuthnCredential> {
    companion object : DbEntityFactory<AuthWebAuthnCredential>()

    var tenantId: String
    var userId: String
    /** Base64url without padding. */
    var credentialId: String
    /** Stable WebAuthn user handle, Base64url without padding. */
    var userHandle: String
    /** COSE public key encoded as Base64url without padding. */
    var publicKeyCose: String
    var signatureCount: Long
    var transports: String?
    var aaguid: String?
    var attestationFormat: String?
    var backupEligible: Boolean
    var backedUp: Boolean
    var discoverable: Boolean
    var displayName: String
    var createdAt: LocalDateTime
    var lastUsedAt: LocalDateTime?
    var revokedAt: LocalDateTime?
    var version: Long
}
