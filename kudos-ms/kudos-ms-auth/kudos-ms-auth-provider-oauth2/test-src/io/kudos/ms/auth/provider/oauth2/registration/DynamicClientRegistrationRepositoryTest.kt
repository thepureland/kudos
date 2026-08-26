package io.kudos.ms.auth.provider.oauth2.registration

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.claim.model.EffectiveIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolverRegistry
import io.kudos.ms.auth.provider.oauth2.secret.IClientSecretResolver
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure tests for DB model -> Spring Security registration conversion. */
internal class DynamicClientRegistrationRepositoryTest {
    private val identityProviderDao = mock(AuthIdentityProviderDao::class.java)
    private val templateDao = mock(AuthProviderTemplateDao::class.java)
    private val claimMappingService = mock(IIdentityProviderClaimMappingService::class.java)
    private val secretRegistry = ClientSecretResolverRegistry(
        listOf(object : IClientSecretResolver {
            override fun supports(reference: String) = reference.startsWith("test:")
            override fun resolve(reference: String) = if (reference == "test:google") "resolved-secret" else null
        })
    )
    private val repository = DynamicClientRegistrationRepository(
        identityProviderDao,
        templateDao,
        claimMappingService,
        secretRegistry,
    )

    private fun provider(secretRef: String? = "test:google") = AuthIdentityProvider {
        id = "provider-1"
        tenantId = "tenant-1"
        templateId = "template-google"
        code = "google-main"
        displayName = "Google"
        clientId = "client-id"
        clientSecretRef = secretRef
        scopes = "openid,profile email"
        jitPolicy = "DISABLED"
        linkPolicy = "BOUND_ONLY"
        active = true
    }

    private fun template() = AuthProviderTemplate {
        id = "template-google"
        code = "GOOGLE"
        protocol = "OIDC"
        issuer = "https://accounts.google.com"
        authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth"
        tokenUri = "https://oauth2.googleapis.com/token"
        userInfoUri = "https://openidconnect.googleapis.com/v1/userinfo"
        jwkSetUri = "https://www.googleapis.com/oauth2/v3/certs"
        subjectClaim = "sub"
        defaultScopes = "openid profile email"
        active = true
    }

    @Test
    fun findByRegistrationId_buildsDynamicOidcRegistration() {
        val provider = provider()
        `when`(identityProviderDao.findActiveById("provider-1")).thenReturn(provider)
        `when`(templateDao.get("template-google")).thenReturn(template())
        `when`(claimMappingService.getEffective("provider-1", "tenant-1"))
            .thenReturn(EffectiveIdentityProviderClaimMapping("provider-1", listOf("sub")))

        val registration = repository.findByRegistrationId("provider-1")!!

        assertEquals("provider-1", registration.registrationId)
        assertEquals("client-id", registration.clientId)
        assertEquals("resolved-secret", registration.clientSecret)
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, registration.clientAuthenticationMethod)
        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", registration.providerDetails.authorizationUri)
        assertEquals("https://oauth2.googleapis.com/token", registration.providerDetails.tokenUri)
        assertEquals("https://www.googleapis.com/oauth2/v3/certs", registration.providerDetails.jwkSetUri)
        assertEquals("sub", registration.providerDetails.userInfoEndpoint.userNameAttributeName)
        assertTrue(registration.scopes.containsAll(setOf("openid", "profile", "email")))
        assertEquals(
            "{baseUrl}/api/public/auth/external/{registrationId}/callback",
            registration.redirectUri,
        )
    }

    @Test
    fun unresolvedSecretReference_disablesRegistration() {
        val provider = provider("vault:not-installed")
        `when`(identityProviderDao.findActiveById("provider-1")).thenReturn(provider)
        `when`(templateDao.get("template-google")).thenReturn(template())
        `when`(claimMappingService.getEffective("provider-1", "tenant-1"))
            .thenReturn(EffectiveIdentityProviderClaimMapping("provider-1", listOf("sub")))

        assertNull(repository.findByRegistrationId("provider-1"))
    }

    @Test
    fun inactiveOrMissingProvider_isNotRegistered() {
        `when`(identityProviderDao.findActiveById("missing")).thenReturn(null)
        assertNull(repository.findByRegistrationId("missing"))
    }
}
