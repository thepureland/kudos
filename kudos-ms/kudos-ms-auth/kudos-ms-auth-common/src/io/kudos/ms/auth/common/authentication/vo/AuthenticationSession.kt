package io.kudos.ms.auth.common.authentication.vo

import java.io.Serializable
import java.time.Instant

/**
 * Secret-free metadata for one Kudos-issued login session.
 *
 * [id] is a logical authentication-session identifier, not the servlet container session id and
 * not a bearer credential. The actual transport credential remains in its HttpOnly cookie or
 * Authorization header.
 */
data class AuthenticationSession(
    val id: String,
    val tenantId: String,
    val userId: String,
    val username: String? = null,
    val clientId: String? = null,
    val deviceId: String? = null,
    val authTime: Instant,
    val amr: Set<String>,
    val acr: String,
    val riskLevel: String? = null,
    val credentialVersion: Long = 0,
    val loginIp: Long? = null,
    val loginDevice: String? = null,
    val loginBrowser: String? = null,
    val loginOs: String? = null,
    val userAgent: String? = null,
    val createdAt: Instant,
    val lastSeenAt: Instant,
    val idleExpiresAt: Instant,
    val absoluteExpiresAt: Instant,
    val revokedAt: Instant? = null,
    val revokeReason: String? = null,
    val version: Long = 0,
) : Serializable {

    fun isActive(at: Instant = Instant.now()): Boolean =
        revokedAt == null && at.isBefore(idleExpiresAt) && at.isBefore(absoluteExpiresAt)

    companion object {
        private const val serialVersionUID = 1L

        /** HttpSession attribute containing this logical, non-bearer session id. */
        const val HTTP_SESSION_ATTRIBUTE = "_KUDOS_AUTH_SESSION_ID_"

        /**
         * HttpSession attribute naming the subject, so a session store that indexes by principal can find a
         * user's container sessions.
         *
         * The value is Spring Session's `FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME`, repeated
         * as a literal so that setting it costs no dependency on Spring Session in the module that issues
         * sessions — a deployment without a Spring Session store simply carries one unused attribute.
         * `kudos-ms-auth-session-spring` asserts this string still equals the real constant, so the copy cannot
         * drift unnoticed.
         */
        const val PRINCIPAL_INDEX_SESSION_ATTRIBUTE =
            "org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME"

        /** Request attribute containing the authoritative session verified for this request. */
        const val REQUEST_ATTRIBUTE = "_KUDOS_AUTHENTICATION_SESSION_"
    }
}
