package io.kudos.ms.user.core.login.model

import java.time.LocalDateTime

/**
 * One terminal login attempt to be persisted in the login audit log.
 *
 * Authentication code passes this immutable value to the logging subdomain instead of depending
 * on its persistence object. A missing [userId] is intentional: attempts for unknown usernames
 * must be auditable without inventing an account identity.
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginAttempt(

    /** Resolved user id, or null when no account matched. */
    val userId: String?,

    /** Username supplied by the caller. */
    val username: String,

    /** Tenant selected for this login attempt. */
    val tenantId: String,

    /** Time at which the authentication attempt started. */
    val loginTime: LocalDateTime,

    /** Server-observed client IP in BigInt form. */
    val loginIp: Long? = null,

    /** Server-observed client terminal type. */
    val loginDevice: String? = null,

    /** Server-observed browser description. */
    val loginBrowser: String? = null,

    /** Server-observed operating-system description. */
    val loginOs: String? = null,

    /** Raw User-Agent string observed by the server. */
    val userAgent: String? = null,

    /** Whether the attempt reached an authenticated state. */
    val loginSuccess: Boolean,

    /** Stable machine-readable failure code; null for successful attempts. */
    val failureReason: String? = null,
)
