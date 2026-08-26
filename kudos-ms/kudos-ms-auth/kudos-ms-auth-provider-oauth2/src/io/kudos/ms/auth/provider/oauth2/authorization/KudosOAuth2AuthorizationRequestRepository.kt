package io.kudos.ms.auth.provider.oauth2.authorization

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Clock

/** Stateless Spring Security bridge backed by the Kudos authorization-request store. */
open class KudosOAuth2AuthorizationRequestRepository(
    private val store: IExternalAuthorizationRequestStore,
    private val properties: ExternalLoginProperties,
    private val clock: Clock = Clock.systemUTC(),
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        state(request)?.let { state -> store.get(state)?.takeIf { it.state == state } }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val state = requireNotNull(authorizationRequest.state)
            .takeIf(::isValidState)
            ?: throw IllegalArgumentException("OAuth authorization state is invalid")
        val ttlSeconds = properties.authorizationRequestTtlSeconds
        require(ttlSeconds in 1..MAX_TTL_SECONDS) { "Invalid OAuth authorization request TTL" }
        check(store.create(authorizationRequest, clock.instant().plusSeconds(ttlSeconds))) {
            "OAuth authorization request state collision: ${state.length} characters"
        }
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? =
        state(request)?.let { state -> store.consume(state)?.takeIf { it.state == state } }

    private fun state(request: HttpServletRequest): String? =
        request.getParameter(STATE_PARAMETER)?.takeIf(::isValidState)

    private fun isValidState(value: String): Boolean = value.isNotBlank() && value.length <= MAX_STATE_LENGTH

    private companion object {
        const val STATE_PARAMETER = "state"
        const val MAX_STATE_LENGTH = 512
        const val MAX_TTL_SECONDS = 900L
    }
}
