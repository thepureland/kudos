package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.provider.oauth2.state.ExternalLoginState
import io.kudos.ms.auth.provider.oauth2.state.IExternalLoginStateStore
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Adds mandatory PKCE and stores a one-time state -> authentication transaction correlation. */
open class KudosOAuth2AuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
    private val transactionService: IAuthenticationTransactionService,
    private val stateStore: IExternalLoginStateStore,
) : OAuth2AuthorizationRequestResolver {

    private val delegate = DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository).apply {
        setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce())
    }

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        capture(request, delegate.resolve(request))

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? = capture(request, delegate.resolve(request, clientRegistrationId))

    private fun capture(
        request: HttpServletRequest,
        authorizationRequest: OAuth2AuthorizationRequest?,
    ): OAuth2AuthorizationRequest? {
        authorizationRequest ?: return null
        val transactionId = request.getParameter(TRANSACTION_ID_PARAMETER)?.takeIf { it.isNotBlank() }
            ?: return null
        val providerId = authorizationRequest.getAttribute<String>(
            OAuth2AuthorizationRequestAttributes.REGISTRATION_ID,
        ) ?: return null
        val transaction = transactionService.get(transactionId) ?: return null
        check(transaction.method == "external:$providerId" &&
            AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER in transaction.nextActions
        ) { "Authentication transaction is not prepared for provider $providerId" }
        val expiresAt = minOf(transaction.expiresAt, Instant.now().plus(5, ChronoUnit.MINUTES))
        check(
            stateStore.create(
                ExternalLoginState(
                    state = requireNotNull(authorizationRequest.state) { "OAuth authorization state is required" },
                    transactionId = transactionId,
                    providerId = providerId,
                    expiresAt = expiresAt,
                )
            )
        ) { "OAuth state collision" }
        return authorizationRequest
    }

    companion object {
        const val TRANSACTION_ID_PARAMETER = "transactionId"
    }
}

private object OAuth2AuthorizationRequestAttributes {
    const val REGISTRATION_ID = "registration_id"
}
