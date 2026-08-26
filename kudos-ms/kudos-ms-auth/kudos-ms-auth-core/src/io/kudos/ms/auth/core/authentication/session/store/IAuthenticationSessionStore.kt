package io.kudos.ms.auth.core.authentication.session.store

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession

/** Optimistic, absolute-TTL store for Kudos authentication-session metadata. */
interface IAuthenticationSessionStore {
    fun create(session: AuthenticationSession): Boolean
    fun get(id: String): AuthenticationSession?
    fun findByUser(tenantId: String, userId: String): List<AuthenticationSession>
    fun save(session: AuthenticationSession, expectedVersion: Long): AuthenticationSession?
}
