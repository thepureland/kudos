package io.kudos.ms.auth.provider.oauth2.authorization

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Single-node fallback used only when the deployment has no Kudos Redis runtime. */
open class InMemoryExternalAuthorizationRequestStore(
    private val clock: Clock = Clock.systemUTC(),
) : IExternalAuthorizationRequestStore {
    private val requests = ConcurrentHashMap<String, Entry>()

    override fun create(
        authorizationRequest: OAuth2AuthorizationRequest,
        expiresAt: Instant,
    ): Boolean {
        removeExpired()
        val state = requireNotNull(authorizationRequest.state) { "OAuth authorization state is required" }
        return requests.putIfAbsent(state, Entry(authorizationRequest, expiresAt)) == null
    }

    override fun get(state: String): OAuth2AuthorizationRequest? {
        val now = clock.instant()
        return requests.computeIfPresent(state) { _, entry ->
            entry.takeIf { now.isBefore(it.expiresAt) }
        }?.authorizationRequest
    }

    override fun consume(state: String): OAuth2AuthorizationRequest? =
        requests.remove(state)?.takeIf { clock.instant().isBefore(it.expiresAt) }?.authorizationRequest

    override fun delete(state: String) {
        requests.remove(state)
    }

    private fun removeExpired() {
        val now = clock.instant()
        requests.entries.removeIf { !now.isBefore(it.value.expiresAt) }
    }

    private data class Entry(
        val authorizationRequest: OAuth2AuthorizationRequest,
        val expiresAt: Instant,
    )
}
