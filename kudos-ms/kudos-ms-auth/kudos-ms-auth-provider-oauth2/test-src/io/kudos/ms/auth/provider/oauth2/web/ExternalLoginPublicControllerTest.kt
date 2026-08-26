package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationReference
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pure boundary tests for tenant-pinned external-login initiation. */
internal class ExternalLoginPublicControllerTest {

    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val transactionService = mock(IAuthenticationTransactionService::class.java)
    private val invitationService = mock(IExternalIdentityInvitationService::class.java)
    private val controller = ExternalLoginPublicController(providerDao, transactionService, invitationService)

    @Test
    fun authorizePinsProviderAndTenantBeforeRedirecting() {
        val provider = AuthIdentityProvider {
            id = "provider-1"
            tenantId = "tenant-1"
            templateId = "google"
            code = "google-main"
            displayName = "Google"
            clientId = "client-id"
            jitPolicy = "DISABLED"
            linkPolicy = "BOUND_ONLY"
            active = true
        }
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider)
        val response = MockHttpServletResponse()

        controller.authorize("provider-1", "tx with space", null, response)

        verify(transactionService).prepareExternal("tx with space", "provider-1", "tenant-1", null)
        assertEquals(
            "/oauth2/authorization/provider-1?transactionId=tx%20with%20space",
            response.redirectedUrl,
        )
    }

    @Test
    fun authorizeRejectsUnavailableProviderBeforeTouchingTransaction() {
        `when`(providerDao.findActiveById("missing")).thenReturn(null)

        assertFailsWith<IllegalArgumentException> {
            controller.authorize("missing", "tx-1", null, MockHttpServletResponse())
        }
        verifyNoInteractions(transactionService)
    }

    @Test
    fun invitationTokenIsValidatedThenOnlySecretFreeIdIsPinned() {
        val provider = AuthIdentityProvider {
            id = "provider-1"
            tenantId = "tenant-1"
            templateId = "google"
            code = "google-main"
            displayName = "Google"
            clientId = "client-id"
            jitPolicy = "INVITE_ONLY"
            linkPolicy = "BOUND_ONLY"
            active = true
        }
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider)
        `when`(
            invitationService.validateToken("raw-invite-token", "tenant-1", "provider-1")
        ).thenReturn(AuthExternalIdentityInvitationReference("invitation-1"))
        val response = MockHttpServletResponse()

        controller.authorize("provider-1", "tx-1", "raw-invite-token", response)

        verify(transactionService).prepareExternal("tx-1", "provider-1", "tenant-1", "invitation-1")
        assertEquals("/oauth2/authorization/provider-1?transactionId=tx-1", response.redirectedUrl)
        kotlin.test.assertFalse(response.redirectedUrl.orEmpty().contains("raw-invite-token"))
    }
}
