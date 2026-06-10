package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.ability.distributed.discovery.nacos.support.IFeignProviderContextProcess
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Feign context web filter — the "reverse adapter" on the provider side.
 *
 * Counterpart: `GlobalHeaderRequestInterceptor` in `kudos-ability-distributed-client-feign` writes
 * the current thread's [io.kudos.context.core.KudosContext] into outbound request headers on the
 * client side; this filter on the provider side **writes those headers back into** the provider
 * process's KudosContext, so the callee's business code "feels like it's on a continuous call chain".
 *
 * **Already wired via [io.kudos.ability.distributed.discovery.nacos.init.NacosDiscoveryAutoConfiguration]
 * as `FilterRegistrationBean<FeignContextWebFilter>`** — enabled by default, can be disabled with
 * `kudos.ability.distributed.discovery.nacos.feign-context-filter.enabled=false`.
 *
 * Extracts Feign call context info from HTTP request headers and sets it into KudosContext.
 *
 * Core features:
 * 1. Context extraction: pulls tenant ID, subsystem code, trace key, datasource ID, locale, etc. from HTTP headers
 * 2. Context setup: writes the extracted info into the current thread's KudosContext for downstream business logic
 * 3. Extension support: allows extending the context handling via the IFeignProviderContextProcess SPI
 *
 * Handled request headers:
 * - TENANT_ID: tenant ID
 * - SUB_SYS_CODE: subsystem code
 * - TRACE_KEY: trace key, used for distributed tracing
 * - DATASOURCE_ID: datasource ID, used for dynamic datasource switching
 * - LOCAL: locale, in the format "languageCode_countryCode" (e.g. zh_CN)
 *
 * Workflow:
 * - Check whether the request carries a Feign / notification propagation marker ([Consts.RequestHeader.FEIGN_REQUEST] or [Consts.RequestHeader.NOTIFY_REQUEST] non-empty)
 * - Extract context info from request headers
 * - Set it into KudosContext
 * - Invoke extension processors for extra context handling
 * - Continue the filter chain
 *
 * Signature verification (HMAC closing of the trust loop):
 * - When a [FeignContextSignatureVerifier] is supplied (i.e. the provider configures
 *   `kudos.ability.distributed.discovery.nacos.feign-context-filter.context-signature-secret`,
 *   which must equal the client's `kudos.ability.distributed.client.feign.contextSignatureSecret`),
 *   every request that would populate the context must carry a valid
 *   `X-Kudos-Context-Timestamp` / `X-Kudos-Context-Nonce` / `X-Kudos-Context-Signature` triple.
 *   Verification covers the HMAC itself, a timestamp window and nonce replay — see
 *   [FeignContextSignatureVerifier]. Requests that fail (including requests missing the headers)
 *   are rejected with 401 and the context is never written.
 * - When no verifier is configured the legacy trust-the-marker behavior is preserved, but a
 *   one-time WARN is logged so operators know context headers are accepted unauthenticated.
 *
 * Notes:
 * - Plain HTTP requests without the above markers are passed through unchanged
 * - If ClientInfo does not exist, it will be created automatically
 * - The locale string is parsed into a Locale object
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class FeignContextWebFilter(
    private val allowUnmarkedContextHeaders: Boolean = false,
    private val signatureVerifier: FeignContextSignatureVerifier? = null
) : Filter {

    private val log = LogFactory.getLog(this::class)

    /** Ensures the "signature verification disabled" warning is logged at most once per instance. */
    private val unsignedWarningLogged = AtomicBoolean(false)

    /**
     * Run the filter: extract the Feign request's context info.
     *
     * Pulls Feign call context info from the HTTP request headers and sets it into KudosContext.
     *
     * Request gate:
     * - Only parse context when either the FEIGN_REQUEST or NOTIFY_REQUEST header is non-empty (both are explicit propagation markers)
     *
     * Context extraction and setup:
     * 1. Extract from headers: tenant ID, subsystem code, trace key, datasource ID, locale
     * 2. Obtain or create the ClientInfo object
     * 3. Parse the locale string (format: languageCode_countryCode, e.g. zh_CN)
     * 4. Set it into KudosContext
     *
     * Extension handling:
     * - Invoke all IFeignProviderContextProcess implementations for additional context processing
     * - Supports custom extensions, e.g. adding extra context info
     *
     * Notes:
     * - Requests without the propagation markers do not mutate the context
     * - If ClientInfo does not exist, it will be created automatically
     * - The locale string is parsed into a Locale object
     * - A blank datasource ID is not written to the context
     *
     * @param servletRequest HTTP request object
     * @param servletResponse HTTP response object
     * @param filterChain filter chain
     */
    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain
    ) {
        // Non-HTTP requests (rare, but allowed by the Filter contract) pass through — avoid breaking the chain with ClassCastException
        val request = servletRequest as? HttpServletRequest ?: run {
            filterChain.doFilter(servletRequest, servletResponse)
            return
        }
        val isFeign = !request.getHeader(Consts.RequestHeader.FEIGN_REQUEST).isNullOrBlank()
        val isNotify = !request.getHeader(Consts.RequestHeader.NOTIFY_REQUEST).isNullOrBlank()
        if (!isFeign && !isNotify && !allowUnmarkedContextHeaders) {
            filterChain.doFilter(servletRequest, servletResponse)
            return
        }
        if (!verifySignature(request, servletResponse)) {
            return
        }

        val tenantId: String? = request.getHeader(Consts.RequestHeader.TENANT_ID)
        val subSysCode: String? = request.getHeader(Consts.RequestHeader.SUB_SYS_CODE)
        val opKey: String? = request.getHeader(Consts.RequestHeader.TRACE_KEY)
        val dataSourceId: String? = request.getHeader(Consts.RequestHeader.DATASOURCE_ID)
        val local: String? = request.getHeader(Consts.RequestHeader.LOCAL)
        val context = KudosContextHolder.get()
        var clientInfo = context.clientInfo
        if (clientInfo == null) {
            clientInfo = ClientInfo(ClientInfo.Builder())
            context.clientInfo = clientInfo
        }
        if (!local.isNullOrBlank()) {
            val parts = local.split("_").filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                clientInfo.locale = Locale.of(parts[0], parts[1])
            }
        }
        context.tenantId = tenantId
        context.subSystemCode = subSysCode
        context.traceKey = opKey
        if (!dataSourceId.isNullOrBlank()) {
            context.dataSourceId = dataSourceId
        }
        SpringKit.getBeansOfType<IFeignProviderContextProcess>().values.forEach { processor ->
            try {
                processor.processContext(request, context)
            } catch (e: Exception) {
                log.error(e, "IFeignProviderContextProcess execution failed: {0}", processor.javaClass.name)
            }
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }

    /**
     * Enforce HMAC verification of the context-propagation headers when a verifier is configured.
     *
     * Returns true when processing may continue (verification passed, or no verifier configured —
     * legacy mode). Returns false after the request has been rejected with 401; the failure WARN
     * only logs the failure reason and request coordinates, never the signature value or the
     * propagated context header values.
     *
     * @param request inbound HTTP request carrying context and signature headers
     * @param servletResponse response used to reject unauthentic requests
     * @return whether the filter should continue restoring the context
     */
    private fun verifySignature(request: HttpServletRequest, servletResponse: ServletResponse?): Boolean {
        val verifier = signatureVerifier
        if (verifier == null) {
            if (unsignedWarningLogged.compareAndSet(false, true)) {
                log.warn(
                    "Feign context signature verification is NOT enabled — context headers " +
                        "(tenant id, datasource id, ...) are trusted without authentication. Configure " +
                        "kudos.ability.distributed.discovery.nacos.feign-context-filter.context-signature-secret " +
                        "(same value as the client's contextSignatureSecret) to enable HMAC verification."
                )
            }
            return true
        }
        val result = verifier.verify(request)
        if (result == FeignContextSignatureVerifier.Result.OK) {
            return true
        }
        // Deliberately do not log signature/nonce values or context header values (tenant id etc.).
        log.warn(
            "Rejected context-propagation request: signature verification failed ({0}), " +
                "method={1}, uri={2}, remoteAddr={3}, remoteHost={4}",
            result, request.method, request.requestURI, request.remoteAddr, request.remoteHost
        )
        val response = servletResponse as? HttpServletResponse
        response?.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Kudos context signature verification failed")
        return false
    }
}
