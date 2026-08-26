package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.state.IExternalLoginStateStore
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.web.util.UriComponentsBuilder

/** Handles protocol/token/nonce failures without reflecting upstream error descriptions. */
open class ExternalLoginAuthenticationFailureHandler(
    private val stateStore: IExternalLoginStateStore,
    private val transactionService: IAuthenticationTransactionService,
    private val properties: ExternalLoginProperties,
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val state = request.getParameter("state")?.let(stateStore::consume)
        val purpose = state?.let { transactionService.get(it.transactionId)?.purpose }
        state?.let {
            runCatching {
                transactionService.failExternal(it.transactionId, it.providerId, "EXTERNAL_AUTHENTICATION_FAILED")
            }
        }
        val path = if (purpose == AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY) {
            properties.linkFailurePath
        } else {
            properties.failurePath
        }
        val builder = UriComponentsBuilder.fromPath(properties.requireSafeLocalPath(path))
            .queryParam("authenticationError", "EXTERNAL_AUTHENTICATION_FAILED")
        state?.let { builder.queryParam("authenticationTransactionId", it.transactionId) }
        response.sendRedirect(builder.build(true).toUriString())
    }
}
