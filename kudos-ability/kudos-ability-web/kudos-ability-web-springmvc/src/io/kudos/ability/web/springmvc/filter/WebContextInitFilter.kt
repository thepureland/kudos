package io.kudos.ability.web.springmvc.filter

import io.kudos.ability.web.common.trace.TraceKeys
import io.kudos.ability.web.springmvc.init.SpringMvcProperties
import io.kudos.ability.web.springmvc.support.getBrowserInfo
import io.kudos.ability.web.springmvc.support.getOsInfo
import io.kudos.ability.web.springmvc.support.getRemoteIp
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher


/**
 * Default implementation of the web context initialization filter.
 *
 * Before each request enters business logic:
 *  1. Copy session / cookie / header entries into [KudosContext] so business code can read them via [KudosContextHolder].
 *  2. Resolve the `traceKey` request header (generate a new UUID when missing or unacceptable) and echo it back
 *     on the response.
 *  3. Resolve client IP / browser / OS / Referer / Locale and assemble a [ClientInfo].
 *  4. Call `KudosContextHolder.clear()` in finally after the request completes, to avoid context leakage when threads are reused by a thread pool.
 *
 * The implementation favours business readability over performance: it copies the entire session into the context,
 * which is unfriendly for large sessions. Requests that gain nothing from that work — static resources, health
 * probes — can be listed under `kudos.ability.web.springmvc.context.exclude-path-patterns`; business code that
 * needs finer control may subclass and override [doFilter] or provide a custom [IWebContextInitFilter] bean.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class WebContextInitFilter(
    private val properties: SpringMvcProperties.Context = SpringMvcProperties.Context()
) : IWebContextInitFilter {

    private val pathMatcher = AntPathMatcher()

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        // Not every ServletRequest is an HttpServletRequest. The previous unchecked cast made that assumption
        // load-bearing, so any non-HTTP dispatch would have failed here with a ClassCastException rather than
        // simply passing through.
        val httpRequest = request as? HttpServletRequest
        if (httpRequest == null) {
            chain.doFilter(request, response)
            return
        }

        try {
            if (!isExcluded(httpRequest)) {
                val context = buildContext(httpRequest)
                KudosContextHolder.set(context)
                writeTraceHeader(response, context.traceKey)
            }
            chain.doFilter(request, response)
        } finally {
            // Clean up all context after the request completes to avoid context pollution and memory leaks when
            // threads are reused by a thread pool.
            //
            // Deliberately outside the `isExcluded` check: `KudosContextHolder.get()` *creates* a context when
            // the thread has none, so even an excluded request leaves one behind the moment anything downstream
            // reads the holder. Skipping the clear for those paths would turn the optimisation into a leak.
            KudosContextHolder.clear()
        }
    }

    /**
     * Assemble the [KudosContext] for the given request.
     *
     * @param request the current request
     * @return the populated context
     * @author K
     * @since 1.0.0
     */
    private fun buildContext(request: HttpServletRequest): KudosContext {
        val context = KudosContext()

        // session: getSession(false) so a context-copy filter never *creates* sessions —
        // the no-arg getter would allocate one per anonymous request (and with spring-session,
        // write it to Redis + emit a Set-Cookie), which is not this filter's business.
        request.getSession(false)?.let { session ->
            session.attributeNames.toList().forEach { name ->
                context.addSessionAttributes(name to session.getAttribute(name))
            }
        }

        // cookie
        request.cookies?.forEach { cookie ->
            context.addCookieAttributes(cookie.name to cookie.value)
        }

        // header
        request.headerNames.toList().forEach { name ->
            context.addHeaderAttributes(name to request.getHeader(name))
        }
        context.traceKey = resolveTraceKey(request)

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

        return context
    }

    /**
     * Resolve the trace key for this request, falling back to a generated one.
     *
     * Several headers are consulted because two audiences propagate a trace differently:
     * [SpringMvcProperties.Context.traceIdHeader] (`X-Trace-Id`) is the public name — the one echoed on the
     * response, and the one the Ktor runtime reads — while `_UUID` is the internal cross-process header that
     * Feign puts on service-to-service calls. Reading only the internal name, as this filter used to, meant a
     * caller could read a trace id off the response and had no way to send it back under that name.
     *
     * A caller-supplied value is accepted only if it is short and alphanumeric-ish. That check is not
     * housekeeping: this value is interpolated into log lines and echoed into a response header, so an
     * unvalidated one lets a caller embed CR/LF and thereby forge log entries or split the response headers.
     * Unbounded lengths additionally make a fine denial-of-service lever against any log or metrics backend that
     * indexes by trace id. A header carrying an unacceptable value is skipped rather than fatal, so one bad
     * header does not discard a good value in the next.
     *
     * @param request the current request
     * @return the first acceptable caller-supplied trace key, otherwise a fresh UUID
     * @author K
     * @since 1.0.0
     */
    private fun resolveTraceKey(request: HttpServletRequest): String =
        TraceKeys.resolve(traceKeyHeaders(), properties.maxTraceKeyLength) { request.getHeader(it) }

    /**
     * The request headers to consult, in precedence order.
     *
     * The public header comes first so that the name advertised on the response is the name honoured on the
     * request, whatever it is configured to be — the two directions stay symmetric by construction rather than
     * by two settings happening to agree.
     *
     * @return the header names, public one first; blank entries are dropped
     * @author K
     * @since 1.0.0
     */
    private fun traceKeyHeaders(): List<String> =
        (listOf(properties.traceIdHeader) + properties.additionalTraceKeyHeaders).filter { it.isNotBlank() }

    /**
     * Echo the trace key onto the response.
     *
     * Set before the chain runs, so it survives a response that commits early or an error that short-circuits
     * downstream: those are precisely the responses a caller most needs a trace id for, and they are the ones
     * that never reach the `ApiResponse` body where the id otherwise lives.
     *
     * @param response the current response
     * @param traceKey the resolved trace key
     * @author K
     * @since 1.0.0
     */
    private fun writeTraceHeader(response: ServletResponse, traceKey: String?) {
        if (traceKey.isNullOrBlank() || properties.traceIdHeader.isBlank()) return
        (response as? HttpServletResponse)?.setHeader(properties.traceIdHeader, traceKey)
    }

    /**
     * Whether the request path is configured to skip context assembly.
     *
     * @param request the current request
     * @return true when the path matches one of [SpringMvcProperties.Context.excludePathPatterns]
     * @author K
     * @since 1.0.0
     */
    private fun isExcluded(request: HttpServletRequest): Boolean {
        val patterns = properties.excludePathPatterns
        if (patterns.isEmpty()) return false
        val path = pathWithinApplication(request)
        return patterns.any { pathMatcher.match(it, path) }
    }

    /**
     * The request path with the servlet context prefix removed, so configured patterns stay independent of
     * whatever `server.servlet.context-path` a deployment happens to use.
     *
     * @param request the current request
     * @return the path within the application, always starting with `/`
     * @author K
     * @since 1.0.0
     */
    private fun pathWithinApplication(request: HttpServletRequest): String {
        val uri = request.requestURI
        val contextPath = request.contextPath
        if (contextPath.isEmpty() || !uri.startsWith(contextPath)) return uri
        return uri.substring(contextPath.length).ifEmpty { "/" }
    }

}
