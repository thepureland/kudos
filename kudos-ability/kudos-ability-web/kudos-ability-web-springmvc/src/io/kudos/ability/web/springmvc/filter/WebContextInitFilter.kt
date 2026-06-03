package io.kudos.ability.web.springmvc.filter

import io.kudos.ability.web.springmvc.support.getBrowserInfo
import io.kudos.ability.web.springmvc.support.getOsInfo
import io.kudos.ability.web.springmvc.support.getRemoteIp
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.support.Consts
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import io.kudos.base.lang.string.RandomStringKit


/**
 * Default implementation of the web context initialization filter.
 *
 * Before each request enters business logic:
 *  1. Copy session / cookie / header entries into [KudosContext] so business code can read them via [KudosContextHolder].
 *  2. Resolve the `traceKey` request header (generate a new UUID when missing).
 *  3. Resolve client IP / browser / OS / Referer / Locale and assemble a [ClientInfo].
 *  4. Call `KudosContextHolder.clear()` in finally after the request completes, to avoid context leakage when threads are reused by a thread pool.
 *
 * The implementation favours business readability over performance: it copies the entire session into the context,
 * which is unfriendly for large sessions. Business code may subclass and override [doFilter] or provide a custom
 * [IWebContextInitFilter] implementation to replace it.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class WebContextInitFilter : IWebContextInitFilter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val context = KudosContext()

        // session
        val session = (request as HttpServletRequest).session
        session.attributeNames.toList().forEach { name ->
            context.addSessionAttributes(name to session.getAttribute(name))
        }

        // cookie
        request.cookies?.forEach { cookie ->
            context.addCookieAttributes(cookie.name to cookie.value)
        }

        // header
        request.headerNames.toList().forEach { name ->
            context.addHeaderAttributes(name to request.getHeader(name))
        }
        context.traceKey = request.getHeader(Consts.RequestHeader.TRACE_KEY)?.takeIf { it.isNotBlank() }
            ?: RandomStringKit.uuid()

        // client info
        val clientInfoBuilder = ClientInfo.Builder()
        clientInfoBuilder.ip(request.getRemoteIp())
        clientInfoBuilder.domain(request.serverName)
        clientInfoBuilder.url(request.requestURI)
        clientInfoBuilder.params(request.parameterMap)
        clientInfoBuilder.browser(request.getBrowserInfo())
        clientInfoBuilder.os(request.getOsInfo())
        clientInfoBuilder.requestReferer(request.getHeader("referer"))
        clientInfoBuilder.locale(request.locale)
//        clientInfoBuilder.timeZone() //TODO
        context.clientInfo = ClientInfo(clientInfoBuilder)


        // initialize context
        KudosContextHolder.set(context)

        try {
            chain.doFilter(request, response)
        } finally {
            // Clean up all context after the request completes to avoid context pollution and memory leaks when threads are reused by a thread pool.
            KudosContextHolder.clear()
        }
    }


}
