package io.kudos.ms.auth.provider.oauth2.state

import java.io.Serializable
import java.time.Instant

/** One-time server-side correlation between OAuth state and a Kudos authentication transaction. */
data class ExternalLoginState(
    val state: String,
    val transactionId: String,
    val providerId: String,
    val expiresAt: Instant,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
