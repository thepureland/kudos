package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderClaimMappingAdminSaveRequest
import io.kudos.ms.auth.core.provider.claim.model.EffectiveIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingSaveCommand
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class IdentityProviderClaimMappingAdminControllerTest {
    private val service = mock(IIdentityProviderClaimMappingService::class.java)
    private val controller = IdentityProviderClaimMappingAdminController(service)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(KudosContext().apply {
            user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
        })
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun savePinsTrustedTenantAndOperator() {
        `when`(service.save(any(IdentityProviderClaimMappingSaveCommand::class.java) ?: fallbackCommand()))
            .thenReturn(EffectiveIdentityProviderClaimMapping("provider-1", listOf("sub"), configured = true))

        val response = controller.save(
            IdentityProviderClaimMappingAdminSaveRequest(
                providerId = "provider-1",
                subjectClaims = listOf("sub"),
                usernameClaims = listOf("profile.username"),
                reason = "Map enterprise claims",
            )
        )

        assertTrue(response.configured)
        val captor = ArgumentCaptor.forClass(IdentityProviderClaimMappingSaveCommand::class.java)
        verify(service).save(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals(listOf("profile.username"), captor.value.usernameClaims)
    }

    @Test
    fun endpointsDeclareConcretePermissions() {
        val get = IdentityProviderClaimMappingAdminController::class.java
            .getDeclaredMethod("get", String::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val save = IdentityProviderClaimMappingAdminController::class.java
            .getDeclaredMethod("save", IdentityProviderClaimMappingAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:identity-provider-claim:view", get.value)
        assertEquals("auth:identity-provider-claim:update", save.value)
    }

    private fun fallbackCommand() = IdentityProviderClaimMappingSaveCommand(
        providerId = "fallback",
        tenantId = "fallback",
        subjectClaims = listOf("sub"),
        usernameClaims = emptyList(),
        displayNameClaims = emptyList(),
        emailClaims = emptyList(),
        emailVerifiedClaims = emptyList(),
        phoneClaims = emptyList(),
        phoneVerifiedClaims = emptyList(),
        avatarClaims = emptyList(),
        localeClaims = emptyList(),
        unionIdClaims = emptyList(),
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
