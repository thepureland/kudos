package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.invitation.model.ExternalIdentityInvitationException
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

/** Validates provider/tenant ownership before handing the browser to Spring Security OAuth2. */
@RestController
@RequestMapping("/api/public/auth/external")
open class ExternalLoginPublicController(
    private val identityProviderDao: AuthIdentityProviderDao,
    private val transactionService: IAuthenticationTransactionService,
    private val invitationService: IExternalIdentityInvitationService,
) {

    @GetMapping("/{providerId}/authorize")
    open fun authorize(
        @PathVariable providerId: String,
        @RequestParam transactionId: String,
        @RequestParam(required = false) invitationToken: String?,
        response: HttpServletResponse,
    ) {
        val provider = requireNotNull(identityProviderDao.findActiveById(providerId)) {
            "External identity provider is not available"
        }
        val invitationId = invitationToken?.takeIf { it.isNotBlank() }?.let { token ->
            try {
                invitationService.validateToken(token, provider.tenantId, provider.id).invitationId
            } catch (e: ExternalIdentityInvitationException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.errorCode, e)
            }
        }
        transactionService.prepareExternal(transactionId, provider.id, provider.tenantId, invitationId)
        val location = UriComponentsBuilder.fromPath("/oauth2/authorization/{providerId}")
            .queryParam(KudosOAuth2AuthorizationRequestResolver.TRANSACTION_ID_PARAMETER, transactionId)
            .buildAndExpand(provider.id)
            .encode()
            .toUriString()
        response.sendRedirect(location)
    }
}
