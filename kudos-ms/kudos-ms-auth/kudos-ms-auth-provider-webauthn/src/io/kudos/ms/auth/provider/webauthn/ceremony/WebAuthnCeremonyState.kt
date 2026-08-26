package io.kudos.ms.auth.provider.webauthn.ceremony

import java.io.Serializable
import java.time.Instant

enum class WebAuthnCeremonyTypeEnum {
    REGISTRATION,
    ASSERTION,
}

/** Secret-free server-side state required to finish exactly one WebAuthn ceremony. */
data class WebAuthnCeremonyState(
    val id: String,
    val type: WebAuthnCeremonyTypeEnum,
    val tenantId: String,
    /** Null only for username-less assertion ceremonies. */
    val userId: String?,
    /** Optional authentication transaction or enrollment workflow that owns this ceremony. */
    val bindingId: String? = null,
    /** Library-owned JSON; restored with the same library version before finish verification. */
    val requestJson: String,
    val createdAt: Instant,
    val expiresAt: Instant,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
