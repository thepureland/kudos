package io.kudos.ms.auth.provider.oauth2.registration

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolverRegistry
import io.kudos.ms.auth.provider.oauth2.support.ProviderEndpointValidator.requireSafeHttps
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ClientRegistrations
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.stereotype.Component

/** Database-backed dynamic Spring Security client-registration repository. */
@Component
open class DynamicClientRegistrationRepository(
    private val identityProviderDao: AuthIdentityProviderDao,
    private val providerTemplateDao: AuthProviderTemplateDao,
    private val claimMappingService: IIdentityProviderClaimMappingService,
    private val secretRegistry: ClientSecretResolverRegistry,
) : ClientRegistrationRepository, Iterable<ClientRegistration> {

    private val log = LogFactory.getLog(this::class)

    override fun findByRegistrationId(registrationId: String): ClientRegistration? {
        val provider = identityProviderDao.findActiveById(registrationId) ?: return null
        val template = providerTemplateDao.get(provider.templateId)?.takeIf { it.active } ?: return null
        return runCatching { build(provider, template) }
            .onFailure { log.warn("Provider registration ${provider.id} is invalid: ${it.message}") }
            .getOrNull()
    }

    override fun iterator(): Iterator<ClientRegistration> =
        identityProviderDao.findAllActive().asSequence()
            .mapNotNull { findByRegistrationId(it.id) }
            .iterator()

    private fun build(
        provider: AuthIdentityProvider,
        template: AuthProviderTemplate,
    ): ClientRegistration {
        require(template.protocol == "OIDC" || template.protocol == "OAUTH2") {
            "Spring OAuth2 engine does not support protocol ${template.protocol}"
        }
        val issuer = provider.issuer ?: template.issuer
        val claimMapping = claimMappingService.getEffective(provider.id, provider.tenantId)
        val secret = secretRegistry.resolve(provider.clientSecretRef)
        require(provider.clientSecretRef.isNullOrBlank() || secret != null) {
            "client_secret_ref could not be resolved"
        }
        val scopes = (provider.scopes ?: template.defaultScopes).orEmpty()
            .split(Regex("[\\s,]+"))
            .filter { it.isNotBlank() }
            .toSet()
        if (template.protocol == "OIDC") require("openid" in scopes) { "OIDC registration requires openid scope" }

        val authorizationUri = template.authorizationUri?.let { requireSafeHttps(it, "authorization_uri") }
        val tokenUri = template.tokenUri?.let { requireSafeHttps(it, "token_uri") }
        val builder = if (authorizationUri != null && tokenUri != null) {
            ClientRegistration.withRegistrationId(provider.id)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
        } else {
            require(template.protocol == "OIDC") {
                "OAuth2 registration requires explicit authorization_uri and token_uri"
            }
            // GENERIC_OIDC instances only need an issuer; Spring resolves the standard discovery
            // document and we then apply the tenant-owned client settings below.
            ClientRegistrations.fromIssuerLocation(
                requireSafeHttps(requireNotNull(issuer) { "OIDC registration requires issuer" }, "issuer")
            ).registrationId(provider.id)
        }

        builder
            .clientName(provider.displayName)
            .clientId(provider.clientId)
            .clientAuthenticationMethod(
                if (secret == null) ClientAuthenticationMethod.NONE else ClientAuthenticationMethod.CLIENT_SECRET_BASIC
            )
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/api/public/auth/external/{registrationId}/callback")
            .scope(scopes)
            .userNameAttributeName(claimMapping.subjectClaims.first())

        secret?.let { builder.clientSecret(it) }
        template.userInfoUri?.let { builder.userInfoUri(requireSafeHttps(it, "user_info_uri")) }
        template.jwkSetUri?.let { builder.jwkSetUri(requireSafeHttps(it, "jwk_set_uri")) }
        issuer?.let { builder.issuerUri(requireSafeHttps(it, "issuer")) }
        return builder.build()
    }
}
