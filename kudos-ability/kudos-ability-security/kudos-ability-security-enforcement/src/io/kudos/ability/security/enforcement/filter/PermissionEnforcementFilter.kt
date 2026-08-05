package io.kudos.ability.security.enforcement.filter

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.ability.security.enforcement.resilience.DecisionGuard
import io.kudos.base.logger.LogFactory
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.util.UrlPathHelper
import org.springframework.web.filter.OncePerRequestFilter


/**
 * URL-level permission enforcement: the point where the authorization model stops being advice and
 * starts being a boundary.
 *
 * Without it the model still renders menus correctly, and a caller who types the URL walks straight
 * past every check the write paths so carefully enforce. Menu filtering hides an action; it does not
 * withhold it.
 *
 * Order of the checks, and why:
 *
 *  1. **public path** — declared, never inferred (see [EnforcementProperties.publicPaths]);
 *  2. **no permission point registered** → refuse. This is the load-bearing decision of the whole
 *     filter. "Unknown endpoint" and "endpoint everyone may call" are different statements, and a
 *     filter that treats an unregistered path as open turns every forgotten registration into a
 *     silent hole — the failure mode that makes people distrust the whole mechanism;
 *  3. **no authenticated caller** → 401, distinct from a caller who is simply not allowed;
 *  4. **credential is stale** → 401. Checked *before* the decision, and that order matters: a token
 *     minted before a revocation still names a subject the decision point will happily answer for,
 *     so asking "may they" first would answer a question about a credential nobody should still be
 *     honouring. 401 rather than 403 because the remedy is to re-authenticate, not to ask for more
 *     permission;
 *  5. **decision point says no** → 403.
 *
 * [EnforcementProperties.shadowMode] converts every refusal into a log line and lets the request
 * through, so an existing deployment can discover its unregistered endpoints from real traffic
 * instead of from an outage.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class PermissionEnforcementFilter(
    private val decisionProvider: IAuthzDecisionProvider,
    private val permissionPointRegistry: IPermissionPointRegistry,
    private val properties: EnforcementProperties,
    /**
     * Null when the deployment has no implementation on the classpath — the check is then simply
     * absent rather than the filter failing to start. Nothing else in the filter depends on it.
     */
    private val tokenFreshnessValidator: ITokenFreshnessValidator? = null,
) : OncePerRequestFilter() {

    private val guard = DecisionGuard(properties.resilience)

    private val log = LogFactory.getLog(this::class)
    private val pathMatcher = AntPathMatcher()
    private val pathHelper = UrlPathHelper.defaultInstance

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = resolvePath(request)
        val method = request.method.uppercase()

        if (isPublic(path)) {
            filterChain.doFilter(request, response)
            return
        }

        // Every collaborator below can reach storage — the registry reloads its snapshot, subject
        // resolution runs an application-supplied SPI, freshness reads the version cache — so the
        // guard wraps the *whole* segment rather than any one of them. Guarding a single call was
        // the earlier mistake: it left three unguarded, and the next one added would have been a
        // fourth. On the caller's thread, because subject resolution reads request-bound state.
        val verdict = guard.guard(
            what = "${method} ${path}",
            onUnavailable = {
                // Indistinguishable from an ordinary refusal on the wire: a probe must not be able
                // to learn from outside that the authorization backend is unwell. Operators get the
                // distinction from the guard's own log line.
                Verdict.Refuse(HttpServletResponse.SC_FORBIDDEN, "decision unavailable for ${method} ${path}")
            },
        ) { evaluate(request, method, path) }

        when (verdict) {
            is Verdict.Allow -> filterChain.doFilter(request, response)
            is Verdict.Refuse -> refuse(request, response, filterChain, verdict.status, verdict.reason)
        }
    }

    /** What the filter decided, and why — computed inside the guard, acted on outside it. */
    private sealed interface Verdict {
        data object Allow : Verdict
        data class Refuse(val status: Int, val reason: String) : Verdict
    }

    /**
     * The decision itself: registered point, authenticated subject, fresh credential, permission.
     *
     * Returns a verdict rather than writing the response, so the whole sequence can sit inside the
     * guard without the guard needing to know anything about servlets.
     */
    private fun evaluate(request: HttpServletRequest, method: String, path: String): Verdict {
        val permissionCode = permissionPointRegistry.resolve(method, path)
            ?: return Verdict.Refuse(
                HttpServletResponse.SC_FORBIDDEN,
                "no permission point is registered for ${method} ${path}",
            )

        if (!decisionProvider.hasSubject()) {
            return Verdict.Refuse(
                HttpServletResponse.SC_UNAUTHORIZED,
                "no authenticated subject for ${method} ${path}",
            )
        }

        if (!isCredentialFresh(request)) {
            return Verdict.Refuse(
                HttpServletResponse.SC_UNAUTHORIZED,
                "credential is out of date for ${method} ${path}; re-authenticate",
            )
        }

        if (!decisionProvider.isPermitted(permissionCode, buildContext(request))) {
            return Verdict.Refuse(
                HttpServletResponse.SC_FORBIDDEN,
                "subject lacks ${permissionCode} for ${method} ${path}",
            )
        }
        return Verdict.Allow
    }

    /**
     * Refuses the request, or — in shadow mode — records what would have happened and continues.
     *
     * Shadow mode covers a guard refusal too, and deliberately so: shadow mode's promise is that
     * *nothing* this filter concludes can stop a request, and an authorization backend going down
     * is exactly the moment that promise has to hold.
     */
    private fun refuse(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
        status: Int,
        reason: String,
    ) {
        if (properties.shadowMode) {
            // Deliberately WARN, not DEBUG: these lines are the migration work-list, and a log level
            // nobody has switched on produces an empty one.
            log.warn("[permission-enforcement][shadow] would deny ${status}: ${reason}")
            filterChain.doFilter(request, response)
            return
        }
        log.debug("[permission-enforcement] denied ${status}: ${reason}")
        response.status = status
        response.contentType = "application/json;charset=UTF-8"
        // The body names the missing permission but not who the caller is or what else they hold —
        // enough for a developer to act on, not enough to probe the model from outside.
        response.writer.write("""{"code":${status},"message":"${escape(reason)}"}""")
    }

    /**
     * Whether the caller's credential still describes their current permissions.
     *
     * Returns true — "do not block on this" — whenever the check is switched off or unavailable, so
     * adding the port to a build changes nothing until somebody opts in.
     */
    private fun isCredentialFresh(request: HttpServletRequest): Boolean {
        if (!properties.tokenFreshness.enabled) return true
        val validator = tokenFreshnessValidator ?: return true
        return when (validator.assess()) {
            ITokenFreshnessValidator.Freshness.FRESH -> true
            ITokenFreshnessValidator.Freshness.STALE -> false
            // The migration state: an unversioned credential is only a refusal once a deployment
            // says its issuer is stamping versions.
            ITokenFreshnessValidator.Freshness.UNKNOWN ->
                !properties.tokenFreshness.rejectUnversionedTokens
        }
    }

    /**
     * The request path this filter judges, resolved the same way Spring routes it.
     *
     * The previous form — `servletPath.ifBlank { requestURI }` — is right in the common case and
     * quietly wrong in the fallback, which fires whenever the dispatcher is mapped somewhere other
     * than the root. `requestURI` is **raw**: it still carries the context path and is neither
     * URL-decoded nor normalised. Two consequences, and the second is the one that matters:
     *
     *  - under a non-root context path nothing matches the registered patterns, so every endpoint is
     *    refused as unregistered — broken, but safely so;
     *  - [publicPaths] is matched against that same raw value, and a raw path can be shaped to match
     *    a public prefix while the container decodes it into something else entirely. Tomcat happens
     *    to reject the encoded-slash forms outright today; that is the container's choice, not this
     *    filter's guarantee, and a security boundary should not rest on it.
     *
     * [UrlPathHelper] is the resolution Spring's own routing and Spring Security use: context path
     * stripped, decoded, semicolon content removed. Judging the same string the router will judge is
     * the only way the two cannot disagree.
     */
    private fun resolvePath(request: HttpServletRequest): String {
        val resolved = pathHelper.getPathWithinApplication(request).ifBlank { "/" }
        // Decoding is not normalising, and the gap between them is a bypass. UrlPathHelper turns
        // `%2f` back into a slash but leaves the `..` segments it exposes intact, so a path aimed
        // out of a public prefix still *matches* that prefix and gets waved through — while the
        // container resolves it to a protected handler. cleanPath collapses the dot segments, so
        // what is matched is what will be routed.
        val normalized = StringUtils.cleanPath(resolved)
        // A `..` that survived cleaning cannot be resolved to a real path at all. There is no
        // legitimate request in that shape, and guessing at its meaning is how a filter ends up
        // guessing differently from the router.
        if (normalized.contains("..")) {
            log.warn("[permission-enforcement] refusing unresolvable path: ${escape(resolved)}")
            return UNRESOLVABLE_PATH
        }
        return normalized
    }

    private fun isPublic(path: String): Boolean =
        properties.publicPaths.any { pattern -> pathMatcher.match(pattern, path) }

    /** Attributes conditional bindings may be evaluated against. */
    private fun buildContext(request: HttpServletRequest): Map<String, Any?> {
        val context = mutableMapOf<String, Any?>("ip" to clientIp(request))
        properties.contextHeaders.forEach { header ->
            request.getHeader(header)?.let { context[header] = it }
        }
        return context
    }

    /**
     * Best-effort client address. `X-Forwarded-For` is honoured only because a reverse proxy is the
     * norm; it is caller-supplied and therefore never suitable as the sole basis for a security
     * decision — which is why ip conditions are an additional restriction on a grant, never a grant.
     */
    private fun clientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) return forwarded.split(",").first().trim()
        return request.remoteAddr ?: ""
    }

    companion object {
        /**
         * Stands in for a path that could not be normalised. It matches no permission point and no
         * public pattern, so the ordinary "unregistered endpoint" refusal applies — one refusal
         * path rather than a second, special one that could drift out of step with it.
         */
        private const val UNRESOLVABLE_PATH = "/__unresolvable__"
    }

    private fun escape(text: String): String = text.replace("\"", "'").replace("\n", " ")
}
