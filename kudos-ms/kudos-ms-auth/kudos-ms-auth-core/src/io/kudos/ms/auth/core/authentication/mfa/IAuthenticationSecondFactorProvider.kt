package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest

/**
 * Optional protocol-neutral second factor contributed by a provider module.
 * Implementations must bind verification to [transactionId] and [userId].
 */
interface IAuthenticationSecondFactorProvider {
    fun action(): AuthenticationActionEnum

    fun policyMethod(): MfaMethodEnum

    fun isAvailable(userId: String, tenantId: String): Boolean

    fun verify(
        transactionId: String,
        userId: String,
        tenantId: String,
        request: AuthenticationActionRequest,
    ): SecondFactorVerificationResult
}
