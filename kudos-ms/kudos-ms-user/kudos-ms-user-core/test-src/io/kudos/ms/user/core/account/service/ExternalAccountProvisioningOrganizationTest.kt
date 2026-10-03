package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningException
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.impl.ExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import io.kudos.ms.user.core.security.PLATFORM_TENANT
import io.kudos.ms.user.core.security.organizationMode
import io.kudos.ms.user.core.security.setField
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Pure tests of the organization-mode rule of [ExternalAccountProvisioningService]: no JIT account
 * creation in non-platform tenants; platform tenants and legacy mode provision as before.
 */
internal class ExternalAccountProvisioningOrganizationTest {

    private val userAccountService = mock(IUserAccountService::class.java)
    private val thirdService = mock(IUserAccountThirdService::class.java)
    private val userOrgUserService = mock(IUserOrgUserService::class.java)
    private val userContactWayService = mock(IUserContactWayService::class.java)

    private fun build(modeEnabled: Boolean?): ExternalAccountProvisioningService =
        ExternalAccountProvisioningService(userAccountService, thirdService, userOrgUserService, userContactWayService)
            .also { svc -> if (modeEnabled != null) setField(svc, "organizationMode", organizationMode(modeEnabled)) }

    private fun command(tenantId: String) = ExternalAccountProvisioningCommand(
        username = "alice",
        tenantId = tenantId,
        identityProviderId = "idp-1",
        providerCode = "oidc",
        issuer = "https://issuer.example",
        subject = "subject-1",
    )

    private fun anyAccount(): UserAccount = any(UserAccount::class.java) ?: UserAccount { id = "x" }
    private fun anyBinding(): ExternalAccountBindingCommand = any(ExternalAccountBindingCommand::class.java)
        ?: ExternalAccountBindingCommand("u", "t", "i", "p", null, "s")

    private fun stubProvisioning(command: ExternalAccountProvisioningCommand): UserAccountThird {
        whenCalled(
            thirdService.getByIdentityProviderSubject(command.tenantId, command.identityProviderId, command.issuer, command.subject)
        ).thenReturn(null)
        whenCalled(userAccountService.insert(anyAccount())).thenReturn("u-jit")
        val binding = UserAccountThird { id = "binding"; userId = "u-jit"; active = true }
        whenCalled(thirdService.jitBindExternalIdentity(anyBinding())).thenReturn(binding)
        return binding
    }

    @Test
    fun organizationModeRefusesJitInNonPlatformTenants() {
        val command = command("tenant-a")
        stubProvisioning(command)
        val e = assertFailsWith<ExternalAccountProvisioningException> { build(true).provision(command) }
        assertEquals("EXTERNAL_JIT_DISABLED_IN_ORGANIZATION_MODE", e.errorCode)
        verify(userAccountService, never()).insert(anyAccount())
        verify(thirdService, never()).jitBindExternalIdentity(anyBinding())
        verifyNoInteractions(userOrgUserService, userContactWayService)
    }

    @Test
    fun organizationModeStillProvisionsInPlatformTenants() {
        val command = command(PLATFORM_TENANT)
        val binding = stubProvisioning(command)
        assertSame(binding, build(true).provision(command))
        verify(userAccountService).insert(anyAccount())
    }

    @Test
    fun modeOffProvisionsAsBefore() {
        val command = command("tenant-a")
        val binding = stubProvisioning(command)
        assertSame(binding, build(false).provision(command))
        verify(userAccountService).insert(anyAccount())
    }

    @Test
    fun withoutOrganizationModeBeanProvisionsAsBefore() {
        val command = command("tenant-a")
        val binding = stubProvisioning(command)
        assertSame(binding, build(null).provision(command))
    }

    @Test
    fun existingActiveBindingStillSignsInWhenModeIsOn() {
        val command = command("tenant-a")
        val existing = UserAccountThird { id = "bound"; userId = "m1"; active = true }
        whenCalled(
            thirdService.getByIdentityProviderSubject(command.tenantId, command.identityProviderId, command.issuer, command.subject)
        ).thenReturn(existing)
        assertSame(existing, build(true).provision(command))
        verify(userAccountService, never()).insert(anyAccount())
    }

    @Test
    fun disabledBindingIsReportedBeforeTheOrganizationRule() {
        val command = command("tenant-a")
        whenCalled(
            thirdService.getByIdentityProviderSubject(command.tenantId, command.identityProviderId, command.issuer, command.subject)
        ).thenReturn(UserAccountThird { id = "bound"; userId = "m1"; active = false })
        val e = assertFailsWith<ExternalAccountProvisioningException> { build(true).provision(command) }
        assertEquals("EXTERNAL_IDENTITY_DISABLED", e.errorCode)
    }
}
