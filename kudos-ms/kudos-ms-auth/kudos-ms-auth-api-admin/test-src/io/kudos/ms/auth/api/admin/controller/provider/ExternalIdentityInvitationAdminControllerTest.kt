package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityInvitationAdminCreateRequest
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityInvitationAdminRevokeRequest
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreateCommand
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreated
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExternalIdentityInvitationAdminControllerTest {
    private val invitationService = mock(IExternalIdentityInvitationService::class.java)
    private val controller = ExternalIdentityInvitationAdminController(invitationService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun createPinsCurrentTenantAndOperatorAndReturnsOneTimeToken() {
        val expiresAt = LocalDateTime.now().plusDays(1)
        `when`(
            invitationService.create(
                any(AuthExternalIdentityInvitationCreateCommand::class.java) ?: fallbackCommand()
            )
        ).thenReturn(AuthExternalIdentityInvitationCreated("invitation-1", "raw-token", expiresAt, 1))

        val response = controller.create(
            ExternalIdentityInvitationAdminCreateRequest(
                userId = "u-1",
                providerId = "provider-1",
                expectedEmail = "alice@example.com",
                expiresAt = expiresAt,
                reason = "Approved onboarding",
            )
        )

        assertEquals("raw-token", response.token)
        val captor = ArgumentCaptor.forClass(AuthExternalIdentityInvitationCreateCommand::class.java)
        verify(invitationService).create(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("provider-1", captor.value.identityProviderId)
    }

    @Test
    fun revokePinsCurrentTenantAndOperator() {
        `when`(
            invitationService.revoke(
                "invitation-1", "tenant-1", "admin-1", "Onboarding cancelled"
            )
        ).thenReturn(true)

        val result = controller.revoke(
            ExternalIdentityInvitationAdminRevokeRequest("invitation-1", "Onboarding cancelled")
        )

        assertEquals(true, result)
        verify(invitationService).revoke(
            "invitation-1", "tenant-1", "admin-1", "Onboarding cancelled"
        )
    }

    @Test
    fun endpointsDeclareConcretePermissions() {
        val create = ExternalIdentityInvitationAdminController::class.java
            .getDeclaredMethod("create", ExternalIdentityInvitationAdminCreateRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val revoke = ExternalIdentityInvitationAdminController::class.java
            .getDeclaredMethod("revoke", ExternalIdentityInvitationAdminRevokeRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:external-invitation:create", create.value)
        assertEquals("auth:external-invitation:revoke", revoke.value)
    }

    private fun fallbackCommand() = AuthExternalIdentityInvitationCreateCommand(
        tenantId = "fallback",
        userId = "fallback",
        identityProviderId = "fallback",
        expiresAt = LocalDateTime.now().plusHours(1),
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
