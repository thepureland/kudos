package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.provider.webauthn.authentication.WebAuthnAuthenticationMethodProvider
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionStart
import io.kudos.ms.auth.provider.webauthn.service.WebAuthnAssertionService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Starts a transaction-bound WebAuthn assertion; completion uses the generic VERIFY_PASSKEY action. */
@RestController
@RequestMapping("/api/public/auth/authentication/transactions")
@ConditionalOnProperty(prefix = "kudos.ms.auth.webauthn", name = ["enabled"], havingValue = "true")
open class WebAuthnAuthenticationPublicController(
    private val transactionService: IAuthenticationTransactionService,
    private val assertionService: WebAuthnAssertionService,
) {

    @PostMapping("/{id}/webauthn/assertion")
    open fun beginAssertion(@PathVariable id: String): WebAuthnAssertionStart {
        val transaction = transactionService.get(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Authentication transaction not found")
        requirePasskeyChallenge(transaction)
        val tenantId = transaction.tenantId
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Authentication transaction has no tenant")
        val expectedUserId = when {
            transaction.purpose == AuthenticationTransactionPurposeEnum.STEP_UP -> transaction.initiatorUserId
                ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Step-up transaction has no subject")
            transaction.method == WebAuthnAuthenticationMethodProvider.METHOD_PASSKEY && transaction.userId == null -> null
            else -> transaction.userId
                ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Second-factor transaction has no subject")
        }
        return assertionService.begin(
            tenantId = tenantId,
            userId = expectedUserId,
            bindingId = transaction.id,
        )
    }

    private fun requirePasskeyChallenge(transaction: AuthenticationTransaction) {
        if (transaction.isTerminal() ||
            AuthenticationActionEnum.VERIFY_PASSKEY !in transaction.nextActions
        ) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Transaction is not awaiting a Passkey assertion")
        }
    }
}
