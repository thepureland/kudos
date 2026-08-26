package io.kudos.ms.auth.core.provider.invitation.service.iservice

import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreateCommand
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreated
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationReference

interface IExternalIdentityInvitationService {
    fun create(command: AuthExternalIdentityInvitationCreateCommand): AuthExternalIdentityInvitationCreated

    /** Validates the bearer token without consuming it, for attachment to a server-side transaction. */
    fun validateToken(
        token: String,
        tenantId: String,
        identityProviderId: String,
    ): AuthExternalIdentityInvitationReference

    /** Atomically consumes the invitation after provider principal validation and returns its local user. */
    fun consume(
        invitationId: String,
        tenantId: String,
        identityProviderId: String,
        principal: ExternalPrincipal,
    ): String

    fun revoke(
        invitationId: String,
        tenantId: String,
        actorUserId: String,
        operationReason: String,
    ): Boolean
}
