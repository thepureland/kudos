package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.impl.ExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ExternalAccountProvisioningServicePureTest {

    private val userAccountService = mock(IUserAccountService::class.java)
    private val thirdService = mock(IUserAccountThirdService::class.java)
    private val userOrgUserService = mock(IUserOrgUserService::class.java)
    private val userContactWayService = mock(IUserContactWayService::class.java)
    private val service = ExternalAccountProvisioningService(
        userAccountService,
        thirdService,
        userOrgUserService,
        userContactWayService,
    )

    @Test
    fun provisionCreatesExternalOnlyAccountAndMandatoryJitBinding() {
        val command = command()
        `when`(
            thirdService.getByIdentityProviderSubject(
                command.tenantId, command.identityProviderId, command.issuer, command.subject,
            )
        ).thenReturn(null)
        `when`(userAccountService.insert(any(UserAccount::class.java) ?: fallbackAccount())).thenReturn("u-jit")
        val binding = UserAccountThird { id = "binding-jit"; userId = "u-jit"; active = true }
        `when`(
            thirdService.jitBindExternalIdentity(
                any(ExternalAccountBindingCommand::class.java) ?: fallbackBinding()
            )
        ).thenReturn(binding)

        val result = service.provision(command)

        assertEquals("binding-jit", result.id)
        val accountCaptor = ArgumentCaptor.forClass(UserAccount::class.java)
        verify(userAccountService).insert(accountCaptor.capture() ?: fallbackAccount())
        val account = accountCaptor.value
        assertEquals("alice_0123456789abcdef", account.username)
        assertEquals("tenant-1", account.tenantId)
        assertEquals("", account.loginPassword)
        assertEquals("ja_JP", account.defaultLocale)
        assertEquals("America/Argentina/Buenos_Aires", account.defaultTimezone)
        assertEquals("org-1", account.orgId)
        assertTrue(account.active)
        assertFalse(account.builtIn)
        verify(userOrgUserService).batchBind("org-1", listOf("u-jit"))

        val bindingCaptor = ArgumentCaptor.forClass(ExternalAccountBindingCommand::class.java)
        verify(thirdService).jitBindExternalIdentity(bindingCaptor.capture() ?: fallbackBinding())
        assertEquals("u-jit", bindingCaptor.value.userId)
        assertEquals("subject-1", bindingCaptor.value.subject)
    }

    @Test
    fun provisionPersistsVerifiedEmailAsNormalContactWay() {
        val command = command().copy(emailVerified = true)
        `when`(
            thirdService.getByIdentityProviderSubject(
                command.tenantId, command.identityProviderId, command.issuer, command.subject,
            )
        ).thenReturn(null)
        `when`(userAccountService.insert(any(UserAccount::class.java) ?: fallbackAccount())).thenReturn("u-jit")
        `when`(userContactWayService.insert(any(UserContactWay::class.java) ?: fallbackContact())).thenReturn("contact-1")
        `when`(
            thirdService.jitBindExternalIdentity(
                any(ExternalAccountBindingCommand::class.java) ?: fallbackBinding()
            )
        ).thenReturn(UserAccountThird { id = "binding-jit"; userId = "u-jit"; active = true })

        service.provision(command)

        val captor = ArgumentCaptor.forClass(UserContactWay::class.java)
        verify(userContactWayService).insert(captor.capture() ?: fallbackContact())
        assertEquals("u-jit", captor.value.userId)
        assertEquals("201", captor.value.contactWayDictCode)
        assertEquals("alice@example.com", captor.value.contactWayValue)
        assertEquals("10", captor.value.contactWayStatusDictCode)
        assertTrue(captor.value.active)
    }

    @Test
    fun provisionIsIdempotentWhenBindingAlreadyExists() {
        val command = command()
        val existing = UserAccountThird { id = "binding-existing"; userId = "u-existing"; active = true }
        `when`(
            thirdService.getByIdentityProviderSubject(
                command.tenantId, command.identityProviderId, command.issuer, command.subject,
            )
        ).thenReturn(existing)

        assertEquals(existing, service.provision(command))
        org.mockito.Mockito.verifyNoInteractions(userAccountService)
    }

    private fun command() = ExternalAccountProvisioningCommand(
        username = "alice_0123456789abcdef",
        tenantId = "tenant-1",
        identityProviderId = "provider-1",
        providerCode = "google",
        issuer = "https://accounts.google.com",
        subject = "subject-1",
        displayName = "Alice",
        email = "alice@example.com",
        defaultLocale = "ja_JP",
        defaultTimezone = "America/Argentina/Buenos_Aires",
        defaultOrgId = "org-1",
    )

    private fun fallbackAccount() = UserAccount { id = "fallback" }

    private fun fallbackContact() = UserContactWay { id = "fallback" }

    private fun fallbackBinding() = ExternalAccountBindingCommand(
        userId = "fallback",
        tenantId = "fallback",
        identityProviderId = "fallback",
        providerCode = "fallback",
        issuer = null,
        subject = "fallback",
    )
}
