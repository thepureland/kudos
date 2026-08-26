package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.base.logger.LogFactory
import io.kudos.base.net.IpKit
import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityAuthenticationException
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityAuthenticationService
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityBindingService
import io.kudos.ms.auth.provider.oauth2.state.IExternalLoginStateStore
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.model.ExternalAccountBindingException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.util.UriComponentsBuilder

/** Converts the verified provider principal, completes the Kudos transaction and issues a local session. */
open class ExternalLoginAuthenticationSuccessHandler(
    private val stateStore: IExternalLoginStateStore,
    private val externalIdentityService: ExternalIdentityAuthenticationService,
    private val externalIdentityBindingService: ExternalIdentityBindingService,
    private val transactionService: IAuthenticationTransactionService,
    private val sessionService: IAuthenticationSessionService,
    private val properties: ExternalLoginProperties,
    private val securityContextRepository: SecurityContextRepository,
) : AuthenticationSuccessHandler {

    private val log = LogFactory.getLog(this::class)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val state = request.getParameter("state")?.let(stateStore::consume)
        if (state == null) {
            redirectFailure(response, null, null, "INVALID_OR_REPLAYED_STATE")
            return
        }
        var purpose: AuthenticationTransactionPurposeEnum? = null
        try {
            val oauth = authentication as? OAuth2AuthenticationToken
                ?: throw ExternalIdentityAuthenticationException("EXTERNAL_AUTHENTICATION_INVALID")
            if (oauth.authorizedClientRegistrationId != state.providerId) {
                throw ExternalIdentityAuthenticationException("EXTERNAL_PROVIDER_MISMATCH")
            }
            val pending = transactionService.get(state.transactionId)
                ?: throw ExternalIdentityAuthenticationException("AUTHENTICATION_TRANSACTION_NOT_FOUND")
            purpose = pending.purpose
            if (pending.purpose == AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY) {
                completeLink(request, response, state.transactionId, state.providerId, oauth)
                return
            }
            val identity = externalIdentityService.authenticate(
                providerId = state.providerId,
                oauth2User = oauth.principal,
                loginIp = IpKit.ipv4StringToLong(request.remoteAddr).takeIf { it >= 0 },
                userAgent = request.getHeader("User-Agent"),
                invitationId = pending.externalInvitationId,
            )
            var transaction = transactionService.completeExternal(
                id = state.transactionId,
                providerId = state.providerId,
                userId = identity.userId,
                tenantId = identity.tenantId,
                username = identity.username,
                providerCode = identity.providerCode,
            )
            if (transaction.status == AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED) {
                val location = UriComponentsBuilder.fromPath(properties.requireSafeLocalPath(properties.successPath))
                    .queryParam("authenticationTransactionId", transaction.id)
                    .queryParam("authenticationChallenge", transaction.errorCode ?: "MFA_REQUIRED")
                    .build(true)
                    .toUriString()
                response.sendRedirect(location)
                return
            }
            if (transaction.status != AuthenticationTransactionStatusEnum.COMPLETED) {
                redirectFailure(
                    response,
                    pending.purpose,
                    transaction.id,
                    transaction.errorCode ?: "EXTERNAL_AUTHENTICATION_FAILED",
                )
                return
            }
            val principal = SessionUserPrincipal(identity.userId, identity.tenantId, identity.username)
            val session = request.getSession(true)
            request.changeSessionId()
            val authSession = sessionService.issue(
                AuthenticationSessionIssueCommand(
                    context = requireNotNull(transaction.context),
                    username = identity.username,
                    loginIp = IpKit.ipv4StringToLong(request.remoteAddr).takeIf { it >= 0 },
                    userAgent = request.getHeader("User-Agent"),
                )
            )
            try {
                transaction = transactionService.bindSession(transaction.id, authSession.id)
                session.setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, authSession.id)
                session.setAttribute(KudosContext.SESSION_KEY_USER, principal)
            } catch (e: Exception) {
                sessionService.revokeForUser(
                    authSession.id,
                    identity.tenantId,
                    identity.userId,
                    "SESSION_BIND_FAILED",
                )
                session.invalidate()
                throw e
            }
            // Do not retain OidcUser/raw claims as the long-lived local authentication object.
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, emptyList())
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response)
            val location = UriComponentsBuilder.fromPath(properties.requireSafeLocalPath(properties.successPath))
                .queryParam("authenticationTransactionId", transaction.id)
                .build(true)
                .toUriString()
            response.sendRedirect(location)
        } catch (e: ExternalAccountBindingException) {
            transactionService.failExternal(state.transactionId, state.providerId, e.errorCode)
            redirectFailure(response, purpose, state.transactionId, e.errorCode)
        } catch (e: ExternalIdentityAuthenticationException) {
            transactionService.failExternal(state.transactionId, state.providerId, e.errorCode)
            redirectFailure(response, purpose, state.transactionId, e.errorCode)
        } catch (e: Exception) {
            log.error(e, "External authentication callback failed")
            runCatching {
                transactionService.failExternal(
                    state.transactionId,
                    state.providerId,
                    "EXTERNAL_AUTHENTICATION_FAILED",
                )
            }
            redirectFailure(response, purpose, state.transactionId, "EXTERNAL_AUTHENTICATION_FAILED")
        }
    }

    private fun completeLink(
        request: HttpServletRequest,
        response: HttpServletResponse,
        transactionId: String,
        providerId: String,
        oauth: OAuth2AuthenticationToken,
    ) {
        val session = request.getSession(false)
            ?: throw ExternalAccountBindingException("LOCAL_SESSION_REQUIRED")
        val localPrincipal = session.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: throw ExternalAccountBindingException("LOCAL_SESSION_REQUIRED")
        val pending = transactionService.get(transactionId)
            ?: throw ExternalAccountBindingException("AUTHENTICATION_TRANSACTION_NOT_FOUND")
        if (pending.initiatorUserId != localPrincipal.id || pending.tenantId != localPrincipal.tenantId) {
            throw ExternalAccountBindingException("LINK_INITIATOR_MISMATCH")
        }
        val resolved = externalIdentityService.resolve(providerId, oauth.principal)
        val binding = externalIdentityBindingService.bind(localPrincipal.id, localPrincipal.tenantId, resolved)
        val completed = transactionService.completeExternalLink(transactionId, providerId, localPrincipal.id)
        request.changeSessionId()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated(localPrincipal, null, emptyList())
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response)
        val location = UriComponentsBuilder.fromPath(properties.requireSafeLocalPath(properties.linkSuccessPath))
            .queryParam("authenticationTransactionId", completed.id)
            .queryParam("externalIdentityBindingId", binding.id)
            .build(true)
            .toUriString()
        response.sendRedirect(location)
    }

    private fun redirectFailure(
        response: HttpServletResponse,
        purpose: AuthenticationTransactionPurposeEnum?,
        transactionId: String?,
        errorCode: String,
    ) {
        val path = if (purpose == AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY) {
            properties.linkFailurePath
        } else {
            properties.failurePath
        }
        val builder = UriComponentsBuilder.fromPath(properties.requireSafeLocalPath(path))
            .queryParam("authenticationError", errorCode)
        transactionId?.let { builder.queryParam("authenticationTransactionId", it) }
        response.sendRedirect(builder.build(true).toUriString())
    }
}
