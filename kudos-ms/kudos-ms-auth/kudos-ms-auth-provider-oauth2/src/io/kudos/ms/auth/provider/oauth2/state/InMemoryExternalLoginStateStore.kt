package io.kudos.ms.auth.provider.oauth2.state

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Single-node fallback used when the optional module runs without Redis. */
open class InMemoryExternalLoginStateStore : IExternalLoginStateStore {
    private val states = ConcurrentHashMap<String, ExternalLoginState>()

    override fun create(state: ExternalLoginState): Boolean {
        states.entries.removeIf { it.value.expiresAt.isBefore(Instant.now()) }
        return states.putIfAbsent(state.state, state) == null
    }

    override fun consume(state: String): ExternalLoginState? =
        states.remove(state)?.takeIf { Instant.now().isBefore(it.expiresAt) }
}
