package io.kudos.ms.auth.common.authentication.vo

import java.io.Serializable
import java.time.Instant

/**
 * Protocol-neutral authentication facts produced after authentication succeeds.
 *
 * [sessionId] remains null until the transport registers a local session or token family. It is a
 * logical, non-bearer Kudos session id and must never contain a servlet container session id,
 * refresh token or other transport credential.
 */
data class AuthenticationContext(
    val userId: String,
    val tenantId: String,
    val sessionId: String? = null,
    val authTime: Instant,
    val amr: Set<String>,
    val acr: String,
    val credentialVersion: Long = 0,
    val riskLevel: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
