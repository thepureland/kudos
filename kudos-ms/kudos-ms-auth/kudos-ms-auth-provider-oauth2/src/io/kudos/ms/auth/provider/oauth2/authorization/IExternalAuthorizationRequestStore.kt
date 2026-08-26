package io.kudos.ms.auth.provider.oauth2.authorization

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Instant

/** Server-side storage for short-lived OAuth2 authorization request snapshots. */
interface IExternalAuthorizationRequestStore {
    fun create(
        authorizationRequest: OAuth2AuthorizationRequest,
        expiresAt: Instant,
    ): Boolean

    fun get(state: String): OAuth2AuthorizationRequest?

    /** Atomically returns and deletes the snapshot so a callback cannot replay it. */
    fun consume(state: String): OAuth2AuthorizationRequest?

    fun delete(state: String)
}
