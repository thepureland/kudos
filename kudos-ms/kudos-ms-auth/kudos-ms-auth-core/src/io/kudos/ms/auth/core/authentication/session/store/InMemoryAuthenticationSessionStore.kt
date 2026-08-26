package io.kudos.ms.auth.core.authentication.session.store

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Single-node session metadata store used when Redis is intentionally absent. */
open class InMemoryAuthenticationSessionStore : IAuthenticationSessionStore {
    private val sessions = ConcurrentHashMap<String, AuthenticationSession>()

    override fun create(session: AuthenticationSession): Boolean {
        removeIfAbsolutelyExpired(session.id)
        return sessions.putIfAbsent(session.id, session) == null
    }

    override fun get(id: String): AuthenticationSession? {
        removeIfAbsolutelyExpired(id)
        return sessions[id]
    }

    override fun findByUser(tenantId: String, userId: String): List<AuthenticationSession> {
        sessions.keys.forEach(::removeIfAbsolutelyExpired)
        return sessions.values.filter { it.tenantId == tenantId && it.userId == userId }
    }

    override fun save(session: AuthenticationSession, expectedVersion: Long): AuthenticationSession? {
        var saved: AuthenticationSession? = null
        sessions.computeIfPresent(session.id) { _, current ->
            if (current.version != expectedVersion || !Instant.now().isBefore(current.absoluteExpiresAt)) {
                current
            } else {
                session.copy(version = expectedVersion + 1).also { saved = it }
            }
        }
        return saved
    }

    private fun removeIfAbsolutelyExpired(id: String) {
        sessions.computeIfPresent(id) { _, current ->
            current.takeIf { Instant.now().isBefore(it.absoluteExpiresAt) }
        }
    }
}
