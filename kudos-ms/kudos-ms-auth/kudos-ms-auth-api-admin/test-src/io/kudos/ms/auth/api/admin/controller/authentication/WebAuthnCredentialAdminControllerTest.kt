package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialAuditSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class WebAuthnCredentialAdminControllerTest {
    private val credentials = mock(IWebAuthnCredentialService::class.java)
    private val accounts = mock(IUserAccountService::class.java)
    private val controller = WebAuthnCredentialAdminController(credentials, accounts)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "t-1", "administrator")
            }
        )
        `when`(accounts.getUserRecord("u-1")).thenReturn(UserAccountRow(id = "u-1", tenantId = "t-1"))
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun listPinsAuditQueryToOperatorTenantAndTrustedTargetUser() {
        val summary = mock(WebAuthnCredentialAuditSummary::class.java)
        `when`(credentials.listForAudit("t-1", "u-1")).thenReturn(listOf(summary))

        assertEquals(listOf(summary), controller.list(" u-1 "))

        verify(accounts).getUserRecord("u-1")
        verify(credentials).listForAudit("t-1", "u-1")
    }

    @Test
    fun listHidesCrossTenantAndMissingUsersBehindSameResponse() {
        `when`(accounts.getUserRecord("cross-tenant"))
            .thenReturn(UserAccountRow(id = "cross-tenant", tenantId = "t-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.list("cross-tenant") }
        val missing = assertFailsWith<ResponseStatusException> { controller.list("missing") }

        assertEquals(404, crossTenant.statusCode.value())
        assertEquals(crossTenant.reason, missing.reason)
        verifyNoInteractions(credentials)
    }

    @Test
    fun listRequiresAuthenticatedAdministrator() {
        KudosContextHolder.clear()

        val error = assertFailsWith<ResponseStatusException> { controller.list("u-1") }

        assertEquals(401, error.statusCode.value())
        verifyNoInteractions(credentials)
    }

    @Test
    fun endpointDeclaresDedicatedViewPermission() {
        val permission = WebAuthnCredentialAdminController::class.java
            .getDeclaredMethod("list", String::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:webauthn-credential:view", permission.value)
    }
}
