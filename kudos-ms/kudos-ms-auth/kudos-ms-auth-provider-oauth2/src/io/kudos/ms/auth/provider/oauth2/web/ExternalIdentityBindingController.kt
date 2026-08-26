package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.provider.enums.ExternalLinkPolicyEnum
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.model.ExternalAccountBindingException
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

/** Self-service external identity lifecycle; identity values can enter only through an OAuth callback. */
@RestController
@RequestMapping("/api/auth/external")
open class ExternalIdentityBindingController(
    private val identityProviderDao: AuthIdentityProviderDao,
    private val transactionService: IAuthenticationTransactionService,
    private val bindingService: IUserAccountThirdService,
    private val properties: ExternalLoginProperties,
    private val assuranceVerifier: AuthenticationAssuranceVerifier,
) {

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @GetMapping("/{providerId}/link")
    open fun link(
        @PathVariable providerId: String,
        @RequestParam(name = "reauthenticationTransactionId", required = false)
        _reauthenticationTransactionId: String? = null,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        val provider = identityProviderDao.findActiveById(providerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "External identity provider is not available")
        if (provider.tenantId != principal.tenantId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "External identity provider belongs to another tenant")
        }
        val linkPolicy = runCatching { ExternalLinkPolicyEnum.valueOf(provider.linkPolicy.uppercase()) }.getOrNull()
        if (linkPolicy != ExternalLinkPolicyEnum.MANUAL_CONFIRM) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Provider does not allow self-service linking")
        }
        val transaction = transactionService.createExternalLink(principal.id, principal.tenantId, provider.id)
        val location = UriComponentsBuilder.fromPath("/oauth2/authorization/{providerId}")
            .queryParam(KudosOAuth2AuthorizationRequestResolver.TRANSACTION_ID_PARAMETER, transaction.id)
            .buildAndExpand(provider.id)
            .encode()
            .toUriString()
        response.sendRedirect(location)
    }

    @GetMapping("/bindings")
    open fun bindings(request: HttpServletRequest): List<ExternalIdentityBindingView> {
        val principal = currentPrincipal(request)
        return bindingService.getActiveByUserAccountId(principal.id)
            .filter { it.tenantId == principal.tenantId }
            .map { binding ->
                val provider = binding.identityProviderId?.let(identityProviderDao::findActiveById)
                ExternalIdentityBindingView(
                    bindingId = binding.id,
                    providerId = binding.identityProviderId,
                    providerCode = binding.accountProviderDictCode,
                    providerDisplayName = provider?.displayName ?: binding.accountProviderDictCode,
                    externalDisplayName = binding.externalDisplayName,
                    avatarUrl = binding.avatarUrl,
                    lastLoginTime = binding.lastLoginTime,
                )
            }
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @DeleteMapping("/bindings/{bindingId}")
    open fun unlink(
        @PathVariable bindingId: String,
        @RequestParam(name = "reauthenticationTransactionId", required = false)
        _reauthenticationTransactionId: String? = null,
        request: HttpServletRequest,
    ): Boolean {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return try {
            bindingService.unbindExternalIdentity(bindingId, principal.id, principal.tenantId)
        } catch (e: ExternalAccountBindingException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, e.errorCode, e)
        }
    }

    private fun currentPrincipal(request: HttpServletRequest): SessionUserPrincipal =
        (request.getSession(false)?.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "A local Kudos session is required")

    private fun requireRecentAuthentication(principal: SessionUserPrincipal, request: HttpServletRequest) {
        val session = (request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as? AuthenticationSession)
            ?.takeIf { it.userId == principal.id && it.tenantId == principal.tenantId }
        assuranceVerifier.verify(
            session,
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
            properties.linkReauthenticationMaxAgeSeconds.coerceAtLeast(1),
        )
    }
}
