package io.kudos.ms.auth.core.authentication.spi

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest

/**
 * Extension point for password, OIDC, WebAuthn and industry-specific authentication methods.
 * Implementations must never persist raw credentials in the transaction.
 */
interface IAuthenticationMethodProvider {
    fun method(): String

    fun supports(transaction: AuthenticationTransaction): Boolean = true

    fun begin(
        transaction: AuthenticationTransaction,
        request: AuthenticationTransactionCreateRequest,
    ): AuthenticationChallenge

    fun verify(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationMethodResult
}

/** The next step returned when a method begins. */
data class AuthenticationChallenge(
    val status: AuthenticationTransactionStatusEnum,
    val nextAction: AuthenticationActionEnum,
)

/** Protocol-neutral outcome returned by an authentication method. */
data class AuthenticationMethodResult(
    val outcome: AuthenticationMethodOutcomeEnum,
    val nextAction: AuthenticationActionEnum? = null,
    val terminal: Boolean = false,
    val userId: String? = null,
    val tenantId: String? = null,
    val username: String? = null,
    val amr: Set<String> = emptySet(),
    val acr: String? = null,
    val errorCode: String? = null,
    val nextActions: Set<AuthenticationActionEnum> = emptySet(),
    val postAuthenticationActions: Set<AuthenticationActionEnum> = emptySet(),
)

enum class AuthenticationMethodOutcomeEnum {
    CHALLENGE,
    SUCCESS,
    FAILURE,
}
