package io.kudos.ability.security.enforcement.filter

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.ability.security.enforcement.port.ITrustedAuthorizationContextProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper

/** Admin APIs always enforce authorization, including when general migration/shadow mode is enabled. */
open class AdminAuthorizationFilter(
    decisionProvider: () -> IAuthzDecisionProvider?,
    registry: () -> IPermissionPointRegistry?,
    properties: () -> EnforcementProperties,
    freshnessValidator: () -> ITokenFreshnessValidator? = { null },
    trustedContextProviders: () -> List<ITrustedAuthorizationContextProvider> = { emptyList() },
) : OncePerRequestFilter() {
    private val delegate: PermissionEnforcementFilter? by lazy {
        val decisions = decisionProvider()
        val points = registry()
        if (decisions == null || points == null) null else {
            val configured = properties()
            val mandatory = EnforcementProperties().apply {
                enabled = true
                shadowMode = false
                publicPaths = emptyList()
                tokenFreshness = configured.tokenFreshness
                resilience = configured.resilience
                trustForwardedFor = configured.trustForwardedFor
                // Admin conditional permissions only accept trusted server-side attributes.
                acceptUntrustedContextHeaders = false
            }
            PermissionEnforcementFilter(decisions, points, mandatory, freshnessValidator(), trustedContextProviders())
                .also { it.setBeanName("mandatoryAdminPermissionEnforcement") }
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = StringUtils.cleanPath(UrlPathHelper.defaultInstance.getPathWithinApplication(request))
        return !path.contains("..") && path != "/api/admin" && !path.startsWith("/api/admin/")
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val filter = delegate
        if (filter == null) {
            response.status = HttpServletResponse.SC_SERVICE_UNAVAILABLE
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"code":503,"message":"Admin authorization is unavailable"}""")
            return
        }
        filter.doFilter(request, response, chain)
    }
}
