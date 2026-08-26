package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityInvitationAdminCreateRequest
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityInvitationAdminRevokeRequest
import io.kudos.ms.auth.common.provider.vo.response.ExternalIdentityInvitationAdminCreatedResponse
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreateCommand
import io.kudos.ms.auth.core.provider.invitation.model.ExternalIdentityInvitationException
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Administrator lifecycle for one-time external-identity invitations. */
@RestController
@RequestMapping("/api/admin/auth/externalInvitation")
open class ExternalIdentityInvitationAdminController(
    private val invitationService: IExternalIdentityInvitationService,
) {

    @PostMapping("/create")
    @RequiresPermission("auth:external-invitation:create")
    open fun create(
        @RequestBody request: ExternalIdentityInvitationAdminCreateRequest,
    ): ExternalIdentityInvitationAdminCreatedResponse = translateErrors {
        val operator = currentOperator()
        val created = invitationService.create(
            AuthExternalIdentityInvitationCreateCommand(
                tenantId = operator.tenantId,
                userId = request.userId,
                identityProviderId = request.providerId,
                expectedEmail = request.expectedEmail,
                expiresAt = request.expiresAt,
                actorUserId = operator.id,
                operationReason = request.reason,
            )
        )
        ExternalIdentityInvitationAdminCreatedResponse(
            invitationId = created.invitationId,
            token = created.token,
            expiresAt = created.expiresAt,
            maxUses = created.maxUses,
        )
    }

    @PostMapping("/revoke")
    @RequiresPermission("auth:external-invitation:revoke")
    open fun revoke(@RequestBody request: ExternalIdentityInvitationAdminRevokeRequest): Boolean = translateErrors {
        val operator = currentOperator()
        invitationService.revoke(
            invitationId = request.invitationId,
            tenantId = operator.tenantId,
            actorUserId = operator.id,
            operationReason = request.reason,
        )
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: ExternalIdentityInvitationException) {
        val status = when (e.errorCode) {
            "EXTERNAL_INVITATION_NOT_FOUND", "EXTERNAL_PROVIDER_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND
            "EXTERNAL_PROVIDER_TENANT_MISMATCH", "ACCOUNT_TENANT_MISMATCH" -> HttpStatus.FORBIDDEN
            "EXTERNAL_PROVIDER_NOT_INVITE_ONLY", "ACCOUNT_UNAVAILABLE" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }
}
