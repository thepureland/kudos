package io.kudos.ms.auth.common.authentication.vo

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import java.io.Serializable
import java.time.Instant

/**
 * Public, secret-free snapshot of one authentication transaction.
 *
 * Passwords, OTP values, authorization codes and provider tokens must never be stored here.
 */
data class AuthenticationTransaction(
    val id: String,
    val tenantId: String?,
    val purpose: AuthenticationTransactionPurposeEnum = AuthenticationTransactionPurposeEnum.LOGIN,
    /** Existing local user that initiated a non-login transaction; never accepted from a public request body. */
    val initiatorUserId: String? = null,
    /** Existing logical session being elevated; never contains a servlet session id or bearer credential. */
    val sourceSessionId: String? = null,
    /** Minimum authentication context class that a step-up result must satisfy. */
    val requiredAcr: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val status: AuthenticationTransactionStatusEnum,
    val nextActions: Set<AuthenticationActionEnum> = emptySet(),
    val method: String? = null,
    /** Secret-free invitation id; the bearer token is validated before this server-side value is set. */
    val externalInvitationId: String? = null,
    val amr: Set<String> = emptySet(),
    val acr: String? = null,
    val context: AuthenticationContext? = null,
    val errorCode: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant,
    val version: Long = 0,
    /** Advisory actions that must be completed after a successful login, without retaining credentials. */
    val postAuthenticationActions: Set<AuthenticationActionEnum> = emptySet(),
) : Serializable {

    fun isTerminal(): Boolean = status in TERMINAL_STATUSES

    companion object {
        private const val serialVersionUID = 1L

        private val TERMINAL_STATUSES = setOf(
            AuthenticationTransactionStatusEnum.COMPLETED,
            AuthenticationTransactionStatusEnum.FAILED,
            AuthenticationTransactionStatusEnum.EXPIRED,
            AuthenticationTransactionStatusEnum.CANCELLED,
        )
    }
}
