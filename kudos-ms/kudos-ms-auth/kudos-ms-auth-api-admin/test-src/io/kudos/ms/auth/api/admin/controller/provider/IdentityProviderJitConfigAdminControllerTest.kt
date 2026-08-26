package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderJitConfigAdminSaveRequest
import io.kudos.ms.auth.core.provider.jit.model.EffectiveIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigSaveCommand
import io.kudos.ms.auth.core.provider.jit.service.iservice.IIdentityProviderJitConfigService
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

internal class IdentityProviderJitConfigAdminControllerTest {
    private val service = mock(IIdentityProviderJitConfigService::class.java)
    private val controller = IdentityProviderJitConfigAdminController(service)

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
        `when`(
            service.save(any(IdentityProviderJitConfigSaveCommand::class.java) ?: fallbackCommand())
        ).thenReturn(EffectiveIdentityProviderJitConfig("provider-1", configured = true))

        val response = controller.save(request())

        assertTrue(response.configured)
        val captor = ArgumentCaptor.forClass(IdentityProviderJitConfigSaveCommand::class.java)
        verify(service).save(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("Change K-42", captor.value.operationReason)
    }

    @Test
    fun endpointsDeclareConcretePermissions() {
        val get = IdentityProviderJitConfigAdminController::class.java
            .getDeclaredMethod("get", String::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val save = IdentityProviderJitConfigAdminController::class.java
            .getDeclaredMethod("save", IdentityProviderJitConfigAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:identity-provider-jit:view", get.value)
        assertEquals("auth:identity-provider-jit:update", save.value)
    }

    private fun request() = IdentityProviderJitConfigAdminSaveRequest(
        providerId = "provider-1",
        usernameStrategy = "OPAQUE_HASHED",
        requireVerifiedEmail = true,
        allowedEmailDomains = listOf("example.com"),
        reason = "Change K-42",
    )

    private fun fallbackCommand() = IdentityProviderJitConfigSaveCommand(
        providerId = "fallback",
        tenantId = "fallback",
        usernameStrategy = "OPAQUE_HASHED",
        requireVerifiedEmail = false,
        allowedEmailDomains = emptyList(),
        defaultOrgId = null,
        defaultSupervisorId = null,
        accountTypeDictCode = null,
        accountStatusDictCode = null,
        defaultLocale = null,
        defaultTimezone = null,
        defaultCurrency = null,
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
