package io.kudos.ms.auth.core.authentication.mfa

import java.io.Serializable
import java.time.Instant

/** Server-side pending TOTP enrollment. [encryptedSecret] is never stored as plaintext. */
data class TotpEnrollment(
    val id: String,
    val tenantId: String,
    val userId: String,
    val encryptedSecret: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val failedAttempts: Int = 0,
    val version: Long = 0,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
