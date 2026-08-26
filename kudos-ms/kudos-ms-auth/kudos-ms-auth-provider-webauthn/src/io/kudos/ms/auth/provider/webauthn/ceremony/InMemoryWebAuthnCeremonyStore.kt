package io.kudos.ms.auth.provider.webauthn.ceremony

import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/** Single-node fallback with atomic, one-time consumption. */
open class InMemoryWebAuthnCeremonyStore(
    private val clock: Clock = Clock.systemUTC(),
) : IWebAuthnCeremonyStore {
    private val states = ConcurrentHashMap<String, WebAuthnCeremonyState>()

    override fun create(state: WebAuthnCeremonyState): Boolean {
        evictExpired()
        if (!clock.instant().isBefore(state.expiresAt)) return false
        return states.putIfAbsent(state.id, state) == null
    }

    override fun get(id: String): WebAuthnCeremonyState? = states[id]?.let { state ->
        if (clock.instant().isBefore(state.expiresAt)) state else null.also { states.remove(id, state) }
    }

    override fun consume(id: String): WebAuthnCeremonyState? = states.remove(id)?.takeIf {
        clock.instant().isBefore(it.expiresAt)
    }

    private fun evictExpired() {
        val now = clock.instant()
        states.entries.removeIf { !now.isBefore(it.value.expiresAt) }
    }
}
