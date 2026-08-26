package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import org.springframework.stereotype.Component

/** Discovers optional second-factor implementations without making core depend on protocol modules. */
@Component
open class AuthenticationSecondFactorRegistry(
    providers: List<IAuthenticationSecondFactorProvider>,
) {
    private val byAction = providers.associateBy { it.action() }.also { indexed ->
        require(indexed.size == providers.size) { "Duplicate authentication second-factor action" }
    }

    open fun availableActions(
        userId: String,
        tenantId: String,
        allowedMethods: Set<MfaMethodEnum>,
    ): Set<AuthenticationActionEnum> = byAction.values.asSequence()
        .filter { it.policyMethod() in allowedMethods }
        .filter { provider -> runCatching { provider.isAvailable(userId, tenantId) }.getOrDefault(false) }
        .mapTo(linkedSetOf()) { it.action() }

    open fun supports(action: AuthenticationActionEnum): Boolean = action in byAction

    open fun verify(
        action: AuthenticationActionEnum,
        transactionId: String,
        userId: String,
        tenantId: String,
        request: AuthenticationActionRequest,
    ): SecondFactorVerificationResult = byAction[action]
        ?.verify(transactionId, userId, tenantId, request)
        ?: SecondFactorVerificationResult(false, errorCode = "UNSUPPORTED_SECOND_FACTOR")
}
