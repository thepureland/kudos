package io.kudos.ms.auth.core.authentication.mfa.webauthn.model

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import java.time.LocalDateTime

/** Output of a protocol library after registration ceremony verification. */
data class VerifiedWebAuthnCredentialRegistration(
    val tenantId: String,
    val userId: String,
    val credentialId: String,
    val userHandle: String,
    val publicKeyCose: String,
    val signatureCount: Long,
    val transports: Set<String> = emptySet(),
    val aaguid: String? = null,
    val attestationFormat: String? = null,
    val backupEligible: Boolean = false,
    val backedUp: Boolean = false,
    val discoverable: Boolean = false,
    val displayName: String,
)

/** Output of a protocol library after assertion signature, origin, RP ID and challenge verification. */
data class VerifiedWebAuthnAssertion(
    val tenantId: String,
    val userId: String,
    val credentialId: String,
    val signatureCount: Long,
    val backedUp: Boolean,
)

data class WebAuthnCredentialSummary(
    val id: String,
    val credentialId: String,
    val transports: Set<String>,
    val aaguid: String?,
    val backupEligible: Boolean,
    val backedUp: Boolean,
    val discoverable: Boolean,
    val displayName: String,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime?,
)

data class WebAuthnCredentialRenameRequest(
    val displayName: String,
)

/** Read-only administrator view. Verification material and the raw credential id are excluded. */
data class WebAuthnCredentialAuditSummary(
    val id: String,
    val credentialIdFingerprint: String,
    val transports: Set<String>,
    val aaguid: String?,
    val attestationFormat: String?,
    val backupEligible: Boolean,
    val backedUp: Boolean,
    val discoverable: Boolean,
    val displayName: String,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime?,
    val revokedAt: LocalDateTime?,
    val authenticatorRiskLevel: WebAuthnAuthenticatorRiskLevelEnum =
        WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
    val authenticatorRiskSources: Set<String> = emptySet(),
    val authenticatorRiskStatusCodes: Set<String> = emptySet(),
)

class WebAuthnCredentialException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
