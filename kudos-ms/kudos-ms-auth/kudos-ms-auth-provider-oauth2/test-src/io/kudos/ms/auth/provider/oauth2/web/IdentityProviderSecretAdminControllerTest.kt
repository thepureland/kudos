package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderSecretAdminRequest
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerification
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerificationCommand
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerificationService
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

internal class IdentityProviderSecretAdminControllerTest {
    private val service = mock(IdentityProviderSecretVerificationService::class.java)
    private val controller = IdentityProviderSecretAdminController(service)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(KudosContext().apply {
            user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
        })
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun verifyPinsTenantAndOperatorAndReturnsSafeStatus() {
        `when`(service.verify(any(IdentityProviderSecretVerificationCommand::class.java) ?: fallbackCommand()))
            .thenReturn(result())

        val response = controller.verify(
            IdentityProviderSecretAdminRequest("provider-1", "Validate deployment configuration")
        )

        assertEquals("provider-1", response.providerId)
        assertEquals("vault", response.referenceScheme)
        assertEquals("RESOLVED", response.status)
        val captor = ArgumentCaptor.forClass(IdentityProviderSecretVerificationCommand::class.java)
        verify(service).verify(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals(false, captor.value.refresh)
    }

    @Test
    fun refreshUsesDedicatedPermissionAndRefreshSemantic() {
        `when`(service.verify(any(IdentityProviderSecretVerificationCommand::class.java) ?: fallbackCommand()))
            .thenReturn(result())

        controller.refresh(IdentityProviderSecretAdminRequest("provider-1", "Credential rotated"))

        val captor = ArgumentCaptor.forClass(IdentityProviderSecretVerificationCommand::class.java)
        verify(service).verify(captor.capture() ?: fallbackCommand())
        assertEquals(true, captor.value.refresh)
        assertEquals(
            "auth:identity-provider-secret:verify",
            permission("verify"),
        )
        assertEquals(
            "auth:identity-provider-secret:refresh",
            permission("refresh"),
        )
    }

    private fun permission(name: String) = IdentityProviderSecretAdminController::class.java
        .getDeclaredMethod(name, IdentityProviderSecretAdminRequest::class.java)
        .getAnnotation(RequiresPermission::class.java).value

    private fun result() = IdentityProviderSecretVerification(
        "provider-1",
        "vault",
        ClientSecretResolutionStatus.RESOLVED,
        LocalDateTime.of(2026, 8, 24, 12, 0),
    )

    private fun fallbackCommand() = IdentityProviderSecretVerificationCommand(
        "fallback", "fallback", "fallback", "fallback",
    )
}
