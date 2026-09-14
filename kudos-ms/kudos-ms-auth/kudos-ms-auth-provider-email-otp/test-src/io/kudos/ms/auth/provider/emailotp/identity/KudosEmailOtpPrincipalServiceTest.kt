package io.kudos.ms.auth.provider.emailotp.identity

import io.kudos.ms.auth.provider.emailotp.EmailOtpProperties
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class KudosEmailOtpPrincipalServiceTest {

    private val thirdService = mock(IUserAccountThirdService::class.java)
    private val provisioningService = mock(IExternalAccountProvisioningService::class.java)
    private val accountService = mock(IUserAccountService::class.java)
    private val properties = EmailOtpProperties().apply {
        autoProvision = true
        identityProviderId = "email-provider"
        providerCode = "email_otp"
    }
    private val service = KudosEmailOtpPrincipalService(
        thirdService,
        provisioningService,
        accountService,
        properties,
    )

    @Test
    fun resolvesExistingBindingWithoutProvisioning() {
        val binding = binding("u-existing")
        `when`(thirdService.getByIdentityProviderSubject("tenant-1", "email-provider", ISSUER, EMAIL))
            .thenReturn(binding)
        `when`(accountService.get("u-existing")).thenReturn(activeUser("u-existing"))

        val result = service.resolveOrProvision("tenant-1", " Alice@Example.com ")

        assertEquals("u-existing", result.userId)
        assertEquals("alice", result.username)
        org.mockito.Mockito.verifyNoInteractions(provisioningService)
    }

    @Test
    fun provisionsStableExternalOnlyAccountWithVerifiedEmail() {
        `when`(thirdService.getByIdentityProviderSubject("tenant-1", "email-provider", ISSUER, EMAIL))
            .thenReturn(null)
        `when`(
            provisioningService.provision(
                any(ExternalAccountProvisioningCommand::class.java) ?: fallbackCommand()
            )
        ).thenReturn(binding("u-new"))
        `when`(accountService.get("u-new")).thenReturn(activeUser("u-new"))

        val result = service.resolveOrProvision("tenant-1", EMAIL)

        assertEquals("u-new", result.userId)
        val captor = ArgumentCaptor.forClass(ExternalAccountProvisioningCommand::class.java)
        verify(provisioningService).provision(captor.capture() ?: fallbackCommand())
        val command = captor.value
        assertTrue(command.username.startsWith("email_"))
        assertEquals(32, command.username.length)
        assertEquals(EMAIL, command.subject)
        assertEquals(EMAIL, command.email)
        assertTrue(command.emailVerified)
    }

    @Test
    fun rejectsUnboundEmailWhenAutoProvisioningIsDisabled() {
        properties.autoProvision = false
        `when`(thirdService.getByIdentityProviderSubject("tenant-1", "email-provider", ISSUER, EMAIL))
            .thenReturn(null)

        val error = assertFailsWith<EmailOtpPrincipalException> {
            service.resolveOrProvision("tenant-1", EMAIL)
        }

        assertEquals("EMAIL_OTP_IDENTITY_NOT_BOUND", error.errorCode)
    }

    @Test
    fun rejectsFrozenAccount() {
        `when`(thirdService.getByIdentityProviderSubject("tenant-1", "email-provider", ISSUER, EMAIL))
            .thenReturn(binding("u-frozen"))
        val user = activeUser("u-frozen")
        user.freezeType = "security"
        `when`(accountService.get("u-frozen")).thenReturn(user)

        val error = assertFailsWith<EmailOtpPrincipalException> {
            service.resolveOrProvision("tenant-1", EMAIL)
        }

        assertEquals("EMAIL_OTP_ACCOUNT_UNAVAILABLE", error.errorCode)
    }

    private fun binding(userId: String) = UserAccountThird {
        id = "binding-$userId"
        this.userId = userId
        active = true
    }

    private fun activeUser(userId: String): UserAccount = UserAccount {
        id = userId
        tenantId = "tenant-1"
        username = "alice"
        active = true
    }

    private fun fallbackCommand() = ExternalAccountProvisioningCommand(
        username = "fallback",
        tenantId = "fallback",
        identityProviderId = "fallback",
        providerCode = "fallback",
        issuer = null,
        subject = "fallback",
    )

    private companion object {
        const val EMAIL = "alice@example.com"
        const val ISSUER = "urn:kudos:email-otp"
    }
}
