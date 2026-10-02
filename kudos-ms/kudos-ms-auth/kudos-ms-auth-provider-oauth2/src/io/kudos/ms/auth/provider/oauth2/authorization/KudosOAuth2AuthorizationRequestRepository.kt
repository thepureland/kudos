package io.kudos.ms.auth.provider.oauth2.authorization

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Clock

/** Authorization requests are stored centrally and bound to the initiating browser session. */
open class KudosOAuth2AuthorizationRequestRepository(
    private val store: IExternalAuthorizationRequestStore,
    private val properties: ExternalLoginProperties,
    private val clock: Clock = Clock.systemUTC(),
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        boundState(request)?.let { state -> store.get(state)?.takeIf { it.state == state } }

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
        val session = request.getSession(true)
        synchronized(session) {
            val bindings = bindings(session.getAttribute(BROWSER_STATES)).filterValues { it > clock.millis() }
                .toMutableMap()
            // Bound per-browser storage, including browsers that repeatedly abandon login.
            while (bindings.size >= MAX_BROWSER_STATES) bindings.remove(bindings.minBy { it.value }.key)
            bindings[state] = clock.instant().plusSeconds(ttlSeconds).toEpochMilli()
            session.setAttribute(BROWSER_STATES, HashMap(bindings))
        }
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        val state = boundState(request) ?: return null
        val session = request.getSession(false) ?: return null
        synchronized(session) {
            val bindings = bindings(session.getAttribute(BROWSER_STATES)).toMutableMap()
            if (bindings.remove(state) == null) return null
            session.setAttribute(BROWSER_STATES, HashMap(bindings))
        }
        return store.consume(state)?.takeIf { it.state == state }
    }

    private fun boundState(request: HttpServletRequest): String? {
        val state = state(request) ?: return null
        val session = request.getSession(false) ?: return null
        val expiresAt = bindings(session.getAttribute(BROWSER_STATES))[state] ?: return null
        return state.takeIf { expiresAt > clock.millis() }
    }

    private fun bindings(value: Any?): Map<String, Long> = (value as? Map<*, *>)?.entries
        ?.mapNotNull { (key, expiry) -> if (key is String && expiry is Long) key to expiry else null }
        ?.toMap() ?: emptyMap()

    private fun state(request: HttpServletRequest): String? =
        request.getParameter(STATE_PARAMETER)?.takeIf(::isValidState)

    private fun isValidState(value: String): Boolean = value.isNotBlank() && value.length <= MAX_STATE_LENGTH

    private companion object {
        const val BROWSER_STATES = "kudos.auth.oauth2.browserStates"
        const val MAX_BROWSER_STATES = 8
        const val STATE_PARAMETER = "state"
        const val MAX_STATE_LENGTH = 512
        const val MAX_TTL_SECONDS = 900L
    }
}
