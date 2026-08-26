package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityAdminPrebindRequest
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityAdminUnbindRequest
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.model.AdminExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ExternalIdentityAdminControllerTest {

    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val templateDao = mock(AuthProviderTemplateDao::class.java)
    private val bindingService = mock(IUserAccountThirdService::class.java)
    private val controller = ExternalIdentityAdminController(providerDao, templateDao, bindingService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "t-1", "administrator")
            }
        )
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun prebindUsesProviderAndCurrentOperatorAsTrustedSources() {
        val provider = provider(tenantId = "t-1")
        val binding = UserAccountThird { id = "binding-1" }
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider)
        `when`(templateDao.get("google")).thenReturn(template())
        `when`(
            bindingService.prebindExternalIdentity(
                org.mockito.ArgumentMatchers.any(AdminExternalAccountBindingCommand::class.java)
                    ?: fallbackCommand()
            )
        ).thenReturn(binding)

        val result = controller.prebind(
            ExternalIdentityAdminPrebindRequest(
                userId = "u-1",
                providerId = "provider-1",
                subject = "external-subject",
                reason = "Approved onboarding ticket K-42",
            )
        )

        assertEquals("binding-1", result)
        val captor = ArgumentCaptor.forClass(AdminExternalAccountBindingCommand::class.java)
        verify(bindingService).prebindExternalIdentity(captor.capture() ?: fallbackCommand())
        assertEquals("t-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("google", captor.value.providerCode)
        assertEquals("https://accounts.google.com", captor.value.issuer)
    }

    @Test
    fun prebindRejectsProviderFromAnotherTenant() {
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider(tenantId = "t-2"))

        val error = assertFailsWith<ResponseStatusException> {
            controller.prebind(
                ExternalIdentityAdminPrebindRequest(
                    userId = "u-1",
                    providerId = "provider-1",
                    subject = "external-subject",
                    reason = "Approved request",
                )
            )
        }

        assertEquals(403, error.statusCode.value())
    }

    @Test
    fun unbindUsesCurrentTenantAndOperator() {
        `when`(
            bindingService.adminUnbindExternalIdentity(
                "binding-1",
                "t-1",
                "admin-1",
                "Offboarding request",
            )
        ).thenReturn(true)

        val result = controller.unbind(
            ExternalIdentityAdminUnbindRequest("binding-1", "Offboarding request")
        )

        assertEquals(true, result)
        verify(bindingService).adminUnbindExternalIdentity(
            "binding-1",
            "t-1",
            "admin-1",
            "Offboarding request",
        )
    }

    @Test
    fun lifecycleEndpointsDeclareConcretePermissions() {
        val prebind = ExternalIdentityAdminController::class.java
            .getDeclaredMethod("prebind", ExternalIdentityAdminPrebindRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val unbind = ExternalIdentityAdminController::class.java
            .getDeclaredMethod("unbind", ExternalIdentityAdminUnbindRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:external-identity:prebind", prebind.value)
        assertEquals("auth:external-identity:unbind", unbind.value)
    }

    private fun provider(tenantId: String) = AuthIdentityProvider {
        id = "provider-1"
        this.tenantId = tenantId
        templateId = "google"
        code = "google-main"
        displayName = "Google"
        issuer = "https://accounts.google.com"
        clientId = "client"
        jitPolicy = "ADMIN_PREPROVISIONED"
        linkPolicy = "BOUND_ONLY"
        active = true
    }

    private fun template() = AuthProviderTemplate {
        id = "google"
        code = "GOOGLE"
        protocol = "OIDC"
        issuer = "https://accounts.google.com"
        discoveryUri = "https://accounts.google.com/.well-known/openid-configuration"
        subjectClaim = "sub"
        defaultScopes = "openid,profile,email"
        adapterType = "SPRING_OIDC"
        active = true
    }

    private fun fallbackCommand() = AdminExternalAccountBindingCommand(
        userId = "fallback",
        tenantId = "fallback",
        identityProviderId = "fallback",
        providerCode = "fallback",
        issuer = null,
        subject = "fallback",
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
