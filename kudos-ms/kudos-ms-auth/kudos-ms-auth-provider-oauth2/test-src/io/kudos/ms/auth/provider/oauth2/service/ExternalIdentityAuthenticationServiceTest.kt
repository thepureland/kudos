package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.claim.model.EffectiveIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.auth.core.provider.jit.model.EffectiveIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.service.iservice.IIdentityProviderJitConfigService
import io.kudos.ms.auth.common.provider.enums.ExternalJitUsernameStrategyEnum
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningException
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService
import org.mockito.Mockito.mock
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class ExternalIdentityAuthenticationServiceTest {
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val templateDao = mock(AuthProviderTemplateDao::class.java)
    private val claimMappingService = mock(IIdentityProviderClaimMappingService::class.java)
    private val invitationService = mock(IExternalIdentityInvitationService::class.java)
    private val thirdService = mock(IUserAccountThirdService::class.java)
    private val jitConfigService = mock(IIdentityProviderJitConfigService::class.java)
    private val provisioningService = mock(IExternalAccountProvisioningService::class.java)
    private val userService = mock(IUserAccountService::class.java)
    private val logService = mock(IUserLogLoginService::class.java)
    private val service = ExternalIdentityAuthenticationService(
        providerDao,
        templateDao,
        claimMappingService,
        invitationService,
        thirdService,
        jitConfigService,
        provisioningService,
        userService,
        logService,
    )

    private val provider = AuthIdentityProvider {
        id = "provider-1"
        tenantId = "t-1"
        templateId = "template-google"
        code = "google-main"
        displayName = "Google"
        clientId = "client"
        jitPolicy = "DISABLED"
        linkPolicy = "BOUND_ONLY"
        active = true
    }
    private val template = AuthProviderTemplate {
        id = "template-google"
        code = "GOOGLE"
        protocol = "OIDC"
        issuer = "https://accounts.google.com"
        subjectClaim = "sub"
        active = true
    }
    private val principal = mock(OidcUser::class.java).also {
        `when`(it.subject).thenReturn("external-subject")
        `when`(it.issuer).thenReturn(URI("https://accounts.google.com").toURL())
        `when`(it.attributes).thenReturn(
            mapOf(
                "sub" to "external-subject",
                "email" to "same@example.com",
                "email_verified" to true,
                "name" to "Alice External",
                "preferred_username" to "Alice Example",
                "locale" to "ja-JP",
            )
        )
    }

    private fun arrangeProvider() {
        `when`(providerDao.findActiveById("provider-1")).thenReturn(provider)
        `when`(templateDao.get("template-google")).thenReturn(template)
        `when`(claimMappingService.getEffective("provider-1", "t-1"))
            .thenReturn(EffectiveIdentityProviderClaimMapping("provider-1", listOf("sub")))
        `when`(jitConfigService.getEffective("provider-1", "t-1"))
            .thenReturn(EffectiveIdentityProviderJitConfig("provider-1"))
    }

    @Test
    fun boundIdentityAuthenticatesExactProviderIssuerAndSubject() {
        arrangeProvider()
        val binding = UserAccountThird {
            id = "binding-1"
            userId = "u-1"
            tenantId = "t-1"
            identityProviderId = "provider-1"
            accountProviderDictCode = "google"
            accountProviderIssuer = "https://accounts.google.com"
            subject = "external-subject"
            active = true
        }
        val user = UserAccount {
            id = "u-1"
            tenantId = "t-1"
            username = "alice"
            loginPassword = "unused"
            supervisorId = ""
            active = true
        }
        `when`(
            thirdService.getByIdentityProviderSubject(
                "t-1",
                "provider-1",
                "https://accounts.google.com",
                "external-subject",
            )
        ).thenReturn(binding)
        `when`(userService.get("u-1")).thenReturn(user)

        val result = service.authenticate("provider-1", principal, 1L, "test-agent")

        assertEquals("u-1", result.userId)
        assertEquals("alice", result.username)
        assertEquals("google", result.providerCode)
        assertEquals("external-subject", result.principal.subject)
        verify(thirdService).updateLastLoginTime("binding-1", binding.lastLoginTime!!)
    }

    @Test
    fun verifiedMatchingEmailDoesNotAutoBindOrJitCreate() {
        arrangeProvider()
        `when`(
            thirdService.getByIdentityProviderSubject(
                "t-1",
                "provider-1",
                "https://accounts.google.com",
                "external-subject",
            )
        ).thenReturn(null)
        `when`(
            thirdService.getByProviderSubject(
                "t-1",
                "google",
                "https://accounts.google.com",
                "external-subject",
            )
        ).thenReturn(null)

        val error = assertFailsWith<ExternalIdentityAuthenticationException> {
            service.authenticate("provider-1", principal, null, null)
        }

        assertEquals("EXTERNAL_IDENTITY_NOT_BOUND", error.errorCode)
        verifyNoInteractions(userService)
    }

    @Test
    fun inviteOnlyConsumesPinnedInvitationAndBindsVerifiedPrincipal() {
        provider.jitPolicy = "INVITE_ONLY"
        arrangeProvider()
        arrangeUnboundIdentity()
        `when`(
            invitationService.consume("invite-1", "t-1", "provider-1", service.resolve("provider-1", principal).principal)
        ).thenReturn("u-1")
        val binding = UserAccountThird {
            id = "binding-invited"
            userId = "u-1"
            tenantId = "t-1"
            identityProviderId = "provider-1"
            accountProviderDictCode = "google"
            accountProviderIssuer = "https://accounts.google.com"
            subject = "external-subject"
            active = true
        }
        `when`(
            thirdService.bindExternalIdentity(
                any(ExternalAccountBindingCommand::class.java) ?: fallbackBindingCommand()
            )
        ).thenReturn(binding)
        `when`(userService.get("u-1")).thenReturn(activeUser())

        val result = service.authenticate("provider-1", principal, null, null, "invite-1")

        assertEquals("u-1", result.userId)
        val captor = ArgumentCaptor.forClass(ExternalAccountBindingCommand::class.java)
        verify(thirdService).bindExternalIdentity(captor.capture() ?: fallbackBindingCommand())
        assertEquals("provider-1", captor.value.identityProviderId)
        assertEquals("external-subject", captor.value.subject)
        assertEquals("same@example.com", captor.value.email)
    }

    @Test
    fun inviteOnlyUnboundIdentityRequiresInvitation() {
        provider.jitPolicy = "INVITE_ONLY"
        arrangeProvider()
        arrangeUnboundIdentity()

        val error = assertFailsWith<ExternalIdentityAuthenticationException> {
            service.authenticate("provider-1", principal, null, null)
        }

        assertEquals("EXTERNAL_INVITATION_REQUIRED", error.errorCode)
        verifyNoInteractions(invitationService)
    }

    @Test
    fun jitCreateProvisionsExternalOnlyAccountWithStableUsernameAndLocale() {
        provider.jitPolicy = "JIT_CREATE"
        arrangeProvider()
        `when`(jitConfigService.getEffective("provider-1", "t-1")).thenReturn(
            EffectiveIdentityProviderJitConfig(
                providerId = "provider-1",
                usernameStrategy = ExternalJitUsernameStrategyEnum.EMAIL_LOCAL_PART_HASHED,
                requireVerifiedEmail = true,
                allowedEmailDomains = listOf("example.com"),
                defaultOrgId = "org-jit",
                defaultSupervisorId = "supervisor-jit",
                accountTypeDictCode = "EXT",
                accountStatusDictCode = "NEW",
                defaultLocale = "en_US",
                defaultTimezone = "Asia/Tokyo",
                defaultCurrency = "JPY",
                configured = true,
            )
        )
        arrangeUnboundIdentity()
        val binding = UserAccountThird {
            id = "binding-jit"
            userId = "u-jit"
            tenantId = "t-1"
            identityProviderId = "provider-1"
            accountProviderDictCode = "google"
            accountProviderIssuer = "https://accounts.google.com"
            subject = "external-subject"
            active = true
        }
        `when`(
            provisioningService.provision(
                any(ExternalAccountProvisioningCommand::class.java) ?: fallbackProvisioningCommand()
            )
        ).thenReturn(binding)
        `when`(userService.get("u-jit")).thenReturn(activeUser("u-jit", "alice_example_jit"))

        val result = service.authenticate("provider-1", principal, null, null)

        assertEquals("u-jit", result.userId)
        val captor = ArgumentCaptor.forClass(ExternalAccountProvisioningCommand::class.java)
        verify(provisioningService).provision(captor.capture() ?: fallbackProvisioningCommand())
        assertTrue(captor.value.username.startsWith("same_"))
        assertTrue(captor.value.username.length <= 32)
        assertEquals("en_US", captor.value.defaultLocale)
        assertEquals("Asia/Tokyo", captor.value.defaultTimezone)
        assertEquals("JPY", captor.value.defaultCurrency)
        assertEquals("org-jit", captor.value.defaultOrgId)
        assertEquals("supervisor-jit", captor.value.defaultSupervisorId)
        assertEquals("EXT", captor.value.accountTypeDictCode)
        assertEquals("external-subject", captor.value.subject)
        assertEquals("same@example.com", captor.value.email)
    }

    @Test
    fun jitCreateConcurrentConflictRecoversCommittedWinnerBinding() {
        provider.jitPolicy = "JIT_CREATE"
        arrangeProvider()
        val winner = UserAccountThird {
            id = "binding-winner"
            userId = "u-winner"
            tenantId = "t-1"
            identityProviderId = "provider-1"
            accountProviderDictCode = "google"
            accountProviderIssuer = "https://accounts.google.com"
            subject = "external-subject"
            active = true
        }
        `when`(
            thirdService.getByIdentityProviderSubject(
                "t-1", "provider-1", "https://accounts.google.com", "external-subject",
            )
        ).thenReturn(null, winner)
        `when`(
            thirdService.getByProviderSubject(
                "t-1", "google", "https://accounts.google.com", "external-subject",
            )
        ).thenReturn(null)
        `when`(
            provisioningService.provision(
                any(ExternalAccountProvisioningCommand::class.java) ?: fallbackProvisioningCommand()
            )
        ).thenThrow(ExternalAccountProvisioningException("EXTERNAL_JIT_PROVISIONING_CONFLICT"))
        `when`(userService.get("u-winner")).thenReturn(activeUser("u-winner", "winner"))

        val result = service.authenticate("provider-1", principal, null, null)

        assertEquals("u-winner", result.userId)
        assertEquals("winner", result.username)
    }

    @Test
    fun jitCreateRejectsVerifiedEmailOutsideConfiguredDomain() {
        provider.jitPolicy = "JIT_CREATE"
        arrangeProvider()
        arrangeUnboundIdentity()
        `when`(jitConfigService.getEffective("provider-1", "t-1")).thenReturn(
            EffectiveIdentityProviderJitConfig(
                providerId = "provider-1",
                requireVerifiedEmail = true,
                allowedEmailDomains = listOf("corp.example"),
                configured = true,
            )
        )

        val error = assertFailsWith<ExternalIdentityAuthenticationException> {
            service.authenticate("provider-1", principal, null, null)
        }

        assertEquals("EXTERNAL_JIT_EMAIL_DOMAIN_NOT_ALLOWED", error.errorCode)
        verifyNoInteractions(provisioningService)
    }

    @Test
    fun jitCreateRejectsMalformedVerifiedEmailBeforeDomainMatching() {
        provider.jitPolicy = "JIT_CREATE"
        arrangeProvider()
        arrangeUnboundIdentity()
        `when`(jitConfigService.getEffective("provider-1", "t-1")).thenReturn(
            EffectiveIdentityProviderJitConfig(
                providerId = "provider-1",
                requireVerifiedEmail = true,
                allowedEmailDomains = listOf("example.com"),
                configured = true,
            )
        )
        val malformedAttributes = principal.attributes + ("email" to "same@@example.com")
        val malformedPrincipal = mock(OidcUser::class.java).also {
            `when`(it.subject).thenReturn("external-subject")
            `when`(it.issuer).thenReturn(URI("https://accounts.google.com").toURL())
            `when`(it.attributes).thenReturn(malformedAttributes)
        }

        val error = assertFailsWith<ExternalIdentityAuthenticationException> {
            service.authenticate("provider-1", malformedPrincipal, null, null)
        }

        assertEquals("EXTERNAL_JIT_EMAIL_INVALID", error.errorCode)
        verifyNoInteractions(provisioningService)
    }

    @Test
    fun oauth2ResolveUsesOrderedNestedClaimMapping() {
        val oauthProvider = AuthIdentityProvider {
            id = "provider-1"
            tenantId = "t-1"
            templateId = "template-oauth2"
            code = "enterprise"
            displayName = "Enterprise OAuth"
            issuer = "https://identity.example.com"
            clientId = "client"
            active = true
        }
        val oauthTemplate = AuthProviderTemplate {
            id = "template-oauth2"
            code = "ENTERPRISE"
            protocol = "OAUTH2"
            subjectClaim = "id"
            active = true
        }
        val oauthUser = mock(OAuth2User::class.java)
        `when`(oauthUser.attributes).thenReturn(
            mapOf(
                "identity" to mapOf("stable_id" to "subject-42"),
                "profiles" to listOf(mapOf("email" to "alice@example.com")),
                "flags" to mapOf("email_verified" to "true"),
                "screen_name" to "alice",
            )
        )
        `when`(providerDao.findActiveById("provider-1")).thenReturn(oauthProvider)
        `when`(templateDao.get("template-oauth2")).thenReturn(oauthTemplate)
        `when`(claimMappingService.getEffective("provider-1", "t-1")).thenReturn(
            EffectiveIdentityProviderClaimMapping(
                providerId = "provider-1",
                subjectClaims = listOf("identity.stable_id"),
                usernameClaims = listOf("missing", "screen_name"),
                emailClaims = listOf("profiles.0.email"),
                emailVerifiedClaims = listOf("flags.email_verified"),
            )
        )

        val result = service.resolve("provider-1", oauthUser).principal

        assertEquals("subject-42", result.subject)
        assertEquals("alice", result.username)
        assertEquals("alice@example.com", result.email)
        assertEquals(true, result.emailVerified)
        assertEquals("https://identity.example.com", result.issuer)
    }

    private fun arrangeUnboundIdentity() {
        `when`(
            thirdService.getByIdentityProviderSubject(
                "t-1", "provider-1", "https://accounts.google.com", "external-subject",
            )
        ).thenReturn(null)
        `when`(
            thirdService.getByProviderSubject(
                "t-1", "google", "https://accounts.google.com", "external-subject",
            )
        ).thenReturn(null)
    }

    private fun activeUser(id: String = "u-1", username: String = "alice") = UserAccount {
        this.id = id
        tenantId = "t-1"
        this.username = username
        loginPassword = "unused"
        supervisorId = ""
        active = true
    }

    private fun fallbackBindingCommand() = ExternalAccountBindingCommand(
        userId = "fallback",
        tenantId = "fallback",
        identityProviderId = "fallback",
        providerCode = "fallback",
        issuer = null,
        subject = "fallback",
    )

    private fun fallbackProvisioningCommand() = ExternalAccountProvisioningCommand(
        username = "fallback",
        tenantId = "fallback",
        identityProviderId = "fallback",
        providerCode = "fallback",
        issuer = null,
        subject = "fallback",
    )
}
