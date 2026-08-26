package io.kudos.ms.auth.core.authentication.session.service.iservice

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand

interface IAuthenticationSessionService {
    fun issue(command: AuthenticationSessionIssueCommand): AuthenticationSession
    fun get(id: String): AuthenticationSession?
    fun touch(id: String): AuthenticationSession?
    fun elevateForUser(
        id: String,
        tenantId: String,
        userId: String,
        context: AuthenticationContext,
    ): AuthenticationSession?
    fun listForUser(tenantId: String, userId: String): List<AuthenticationSession>
    fun revokeForUser(id: String, tenantId: String, userId: String, reason: String): AuthenticationSession?
    fun revokeAllForUser(tenantId: String, userId: String, reason: String): List<AuthenticationSession>
}
