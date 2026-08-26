package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.ability.web.springmvc.support.getBrowserInfo
import io.kudos.ability.web.springmvc.support.getClientTerminal
import io.kudos.ability.web.springmvc.support.getOsInfo
import io.kudos.base.net.IpKit
import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationStepUpCreateRequest
import io.kudos.ms.auth.core.authentication.model.AuthenticationStepUpCreateCommand
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** Public transport for resumable authentication transactions. */
@RestController
@RequestMapping("/api/public/auth/authentication/transactions")
open class AuthenticationTransactionPublicController(
    private val transactionService: IAuthenticationTransactionService,
    private val sessionService: IAuthenticationSessionService,
) {

    @PostMapping
    open fun create(
        @RequestBody @Valid request: AuthenticationTransactionCreateRequest,
    ): AuthenticationTransaction = transactionService.create(request)

    /** Starts a fresh, subject-bound re-authentication for the current logical session. */
    @PostMapping("/step-up")
    open fun createStepUp(
        @RequestBody @Valid request: AuthenticationStepUpCreateRequest,
        servletRequest: HttpServletRequest,
    ): AuthenticationTransaction {
        if (request.requiredAcr.isBlank() || request.requestedMethod.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "requiredAcr and requestedMethod must not be blank")
        }
        val httpSession = servletRequest.getSession(false)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated session is required")
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated session is required")
        val sourceSessionId = httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "A managed authentication session is required")
        val source = sessionService.get(sourceSessionId)
        if (source?.isActive() != true || source.userId != principal.id || source.tenantId != principal.tenantId) {
            httpSession.invalidate()
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "The authentication session is no longer active")
        }
        return transactionService.createStepUp(
            AuthenticationStepUpCreateCommand(
                userId = principal.id,
                tenantId = principal.tenantId,
                username = principal.username,
                sourceSessionId = source.id,
                requiredAcr = request.requiredAcr,
                requestedMethod = request.requestedMethod,
            )
        )
    }

    @GetMapping("/{id}")
    open fun get(@PathVariable id: String): AuthenticationTransaction? = transactionService.get(id)

    @PostMapping("/{id}/actions/{action}")
    open fun act(
        @PathVariable id: String,
        @PathVariable action: AuthenticationActionEnum,
        @RequestBody @Valid request: AuthenticationActionRequest,
        servletRequest: HttpServletRequest,
    ): AuthenticationTransaction {
        transactionService.get(id)
            ?.takeIf { it.purpose == AuthenticationTransactionPurposeEnum.STEP_UP }
            ?.let { requireStepUpSource(it, servletRequest) }
        val observedRequest = request.copy(
            loginIp = IpKit.ipv4StringToLong(servletRequest.remoteAddr).takeIf { it >= 0 },
            loginDevice = servletRequest.getClientTerminal(),
            loginBrowser = servletRequest.getBrowserInfo().asClientDescription(),
            loginOs = servletRequest.getOsInfo().asClientDescription(),
            userAgent = servletRequest.getHeader("User-Agent"),
        )
        var transaction = transactionService.act(id, action, observedRequest)
        if (transaction.status == AuthenticationTransactionStatusEnum.COMPLETED) {
            transaction = when (transaction.purpose) {
                AuthenticationTransactionPurposeEnum.LOGIN ->
                    completeLogin(id, transaction, observedRequest, servletRequest)

                AuthenticationTransactionPurposeEnum.STEP_UP ->
                    completeStepUp(id, transaction, servletRequest)

                AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY -> transaction
            }
        }
        return transaction
    }

    @PostMapping("/{id}/cancel")
    open fun cancel(@PathVariable id: String): AuthenticationTransaction? = transactionService.cancel(id)

    private fun completeLogin(
        id: String,
        transaction: AuthenticationTransaction,
        observedRequest: AuthenticationActionRequest,
        servletRequest: HttpServletRequest,
    ): AuthenticationTransaction {
        val context = requireNotNull(transaction.context)
        val session = servletRequest.getSession(true)
        servletRequest.changeSessionId()
        val authSession = sessionService.issue(
            AuthenticationSessionIssueCommand(
                context = context,
                username = transaction.username,
                loginIp = observedRequest.loginIp,
                loginDevice = observedRequest.loginDevice,
                loginBrowser = observedRequest.loginBrowser,
                loginOs = observedRequest.loginOs,
                userAgent = observedRequest.userAgent,
            )
        )
        try {
            val bound = transactionService.bindSession(id, authSession.id)
            session.setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, authSession.id)
            // Lets a principal-indexed session store find this container session when the logical one is
            // revoked. Deployments without such a store simply carry one unused attribute.
            session.setAttribute(AuthenticationSession.PRINCIPAL_INDEX_SESSION_ATTRIBUTE, context.userId)
            session.setAttribute(
                KudosContext.SESSION_KEY_USER,
                SessionUserPrincipal(
                    id = context.userId,
                    tenantId = context.tenantId,
                    username = transaction.username.orEmpty(),
                )
            )
            return bound
        } catch (e: Exception) {
            sessionService.revokeForUser(
                authSession.id,
                context.tenantId,
                context.userId,
                "SESSION_BIND_FAILED",
            )
            session.invalidate()
            throw e
        }
    }

    private fun completeStepUp(
        id: String,
        transaction: AuthenticationTransaction,
        servletRequest: HttpServletRequest,
    ): AuthenticationTransaction {
        val context = requireNotNull(transaction.context)
        val session = requireStepUpSource(transaction, servletRequest)
        val sourceSessionId = requireNotNull(transaction.sourceSessionId)
        val elevated = sessionService.elevateForUser(
            sourceSessionId,
            context.tenantId,
            context.userId,
            context,
        )
        if (elevated == null) {
            session.invalidate()
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "The source session is no longer active")
        }
        servletRequest.changeSessionId()
        return transactionService.bindSession(id, elevated.id)
    }

    private fun requireStepUpSource(
        transaction: AuthenticationTransaction,
        servletRequest: HttpServletRequest,
    ): HttpSession {
        val sourceSessionId = requireNotNull(transaction.sourceSessionId)
        val session = servletRequest.getSession(false)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "The source session is no longer active")
        val principal = session.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
        val currentSessionId = session.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
        if (currentSessionId != sourceSessionId || principal?.id != transaction.initiatorUserId ||
            principal?.tenantId != transaction.tenantId
        ) {
            session.invalidate()
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "The step-up source session does not match")
        }
        return session
    }

    private fun Pair<String, String>.asClientDescription(): String =
        if (second == "unknown") first else "$first $second"
}
