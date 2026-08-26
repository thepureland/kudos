package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderAdminCreateRequest
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderAdminSetActiveRequest
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderAdminUpdateRequest
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderCreateCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderSetActiveCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderUpdateCommand
import io.kudos.ms.auth.core.provider.management.model.ManagedIdentityProvider
import io.kudos.ms.auth.core.provider.management.service.iservice.IIdentityProviderManagementService
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

internal class IdentityProviderAdminControllerTest {
    private val service = mock(IIdentityProviderManagementService::class.java)
    private val controller = IdentityProviderAdminController(service)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(KudosContext().apply {
            user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
        })
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun createPinsTrustedTenantAndOperatorAndMasksSecretReference() {
        `when`(service.create(any(IdentityProviderCreateCommand::class.java) ?: fallbackCreate()))
            .thenReturn(managed())

        val response = controller.create(
            IdentityProviderAdminCreateRequest(
                templateId = "template-1",
                code = "google-main",
                displayName = "Google",
                clientId = "client-id",
                clientSecretRef = "vault:auth/google",
                scopes = listOf("openid", "email"),
                reason = "Enable workforce login",
            )
        )

        assertTrue(response.clientSecretConfigured)
        val captor = ArgumentCaptor.forClass(IdentityProviderCreateCommand::class.java)
        verify(service).create(captor.capture() ?: fallbackCreate())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("vault:auth/google", captor.value.clientSecretRef)
    }

    @Test
    fun endpointsDeclareConcretePermissions() {
        val type = IdentityProviderAdminController::class.java
        assertEquals("auth:identity-provider:view", permission(type, "listTemplates"))
        assertEquals("auth:identity-provider:view", permission(type, "list"))
        assertEquals("auth:identity-provider:view", permission(type, "get", String::class.java))
        assertEquals(
            "auth:identity-provider:create",
            permission(type, "create", IdentityProviderAdminCreateRequest::class.java),
        )
        assertEquals(
            "auth:identity-provider:update",
            permission(type, "update", IdentityProviderAdminUpdateRequest::class.java),
        )
        assertEquals(
            "auth:identity-provider:set-active",
            permission(type, "setActive", IdentityProviderAdminSetActiveRequest::class.java),
        )
    }

    private fun permission(type: Class<*>, name: String, vararg parameters: Class<*>) =
        type.getDeclaredMethod(name, *parameters).getAnnotation(RequiresPermission::class.java).value

    private fun managed() = ManagedIdentityProvider(
        id = "provider-1",
        templateId = "template-1",
        templateCode = "GOOGLE",
        protocol = "OIDC",
        code = "google-main",
        displayName = "Google",
        issuer = "https://accounts.google.com",
        clientId = "client-id",
        clientSecretConfigured = true,
        scopes = listOf("openid", "email"),
        effectiveScopes = listOf("openid", "email"),
        jitPolicy = "DISABLED",
        linkPolicy = "BOUND_ONLY",
        active = false,
    )

    private fun fallbackCreate() = IdentityProviderCreateCommand(
        tenantId = "fallback",
        templateId = "fallback",
        code = "fallback",
        displayName = "fallback",
        issuer = null,
        clientId = "fallback",
        clientSecretRef = null,
        scopes = emptyList(),
        jitPolicy = "DISABLED",
        linkPolicy = "BOUND_ONLY",
        active = false,
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
