package io.kudos.ability.security.enforcement.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * Configuration for URL-level permission enforcement.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.security.enforcement")
open class EnforcementProperties {

    /** Refuse application startup when a production profile leaves enforcement insecure. */
    var failOnInsecureProduction: Boolean = true

    /** Profile names treated as production by the startup safety check. */
    var productionProfiles: Set<String> = setOf("prod", "production")

    /**
     * Whether the filter is installed at all. **Off by default**, and that default is not timidity:
     * switching URL enforcement on before a deployment has registered its permission points would
     * deny every request, since an unregistered path is refused by design. Opt in once the registry
     * is populated — [shadowMode] is how you find out whether it is.
     */
    var enabled: Boolean = false

    /**
     * Log what would have been refused, but let it through.
     *
     * The migration path for an existing system: turn the filter on in shadow mode, drive real
     * traffic, collect the `would deny` lines, register the permission points they name, and only
     * then switch it off. Without this, adopting enforcement is a flag day.
     */
    var shadowMode: Boolean = true

    /**
     * Paths that need no permission at all — health checks, login, static assets. Ant-style
     * patterns are supported, e.g. an `/api/public/` prefix followed by a double-star wildcard.
     *
     * Being public is stated here explicitly. It is never inferred from a path having no permission
     * point registered, because "nobody got round to registering it" and "everyone may call it" must
     * not look the same to the filter.
     */
    var publicPaths: List<String> = listOf(
        "/actuator/**",
        "/error",
    )

    /**
     * Context attribute names to lift out of the request for conditional bindings. `ip` is supplied
     * automatically; anything else listed here is read from the request headers under the same name.
     */
    var contextHeaders: List<String> = emptyList()

    /** Compatibility escape hatch for legacy header-derived ABAC; caller headers are untrusted. */
    var acceptUntrustedContextHeaders: Boolean = false

    /** Trust X-Forwarded-For only behind a sanitising trusted proxy. */
    var trustForwardedFor: Boolean = false

    /**
     * Stops the enforcement path re-paying the storage timeout once the decision point is clearly
     * down. On by default, unlike this module's other switches: it does not change *what* is
     * enforced, only how fast a refusal arrives when the decision point cannot answer.
     */
    var resilience: ResilienceProperties = ResilienceProperties()

    /**
 * @author K
 * @author AI: Codex
 * @author AI: Claude
     * @since 1.0.0
     */
    open class ResilienceProperties {

        /** Whether the breaker applies at all. */
        var enabled: Boolean = true

        /** Consecutive failures after which the decision point is presumed down. */
        var failureThreshold: Int = 5

        /**
         * How long to refuse outright before letting one probe through. Long enough not to hammer a
         * real outage, short enough that recovery is noticed promptly.
         *
         * **Keep this at least as long as a failing call takes.** It was 10s while a failing
         * decision took the datasource's 30s connection timeout, which is worse than it sounds: the
         * window expired long before the in-flight call even returned, so the breaker was half-open
         * again by the time the next request arrived and nearly every request became a probe paying
         * the full 30s. An open window shorter than the failure it protects against protects against
         * very little. Matched to the default HikariCP connection timeout it is measured against.
         *
         * **There is deliberately no per-call timeout setting here.** A call cannot be interrupted
         * by the thread that is blocked on it, and moving the decision to another thread is not
         * available: subject resolution reads request-bound state that only exists on the request
         * thread. The per-call bound belongs where the blocking happens — the datasource's
         * connection and socket timeouts — and this breaker is what stops that bound being paid
         * over and over.
         */
        var openMillis: Long = 30_000
    }

    /**
     * Whether the filter checks that the caller's credential still matches their current
     * permissions (see [io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator]).
     *
     * Off by default and gated separately from [enabled]: it is a check on the *credential*, and a
     * deployment whose tokens do not yet carry a version would refuse every request the moment
     * [rejectUnversionedTokens] were also on. Turn it on, watch the shadow log, then tighten.
     */
    var tokenFreshness: TokenFreshnessProperties = TokenFreshnessProperties()

    /**
 * @author K
 * @author AI: Codex
 * @author AI: Claude
     * @since 1.0.0
     */
    open class TokenFreshnessProperties {

        /** Whether the check runs at all. */
        var enabled: Boolean = false

        /**
         * What to do with a credential that carries no version.
         *
         * **False by default, and that default is the whole migration story.** Every token minted
         * before this feature existed is unversioned; refusing them on day one is a mass logout.
         * Leave it false, let the issuer start stamping versions, watch the shadow log empty out,
         * then set it true — at which point an *absent* version stops being an accident and starts
         * being a forgery signal.
         */
        var rejectUnversionedTokens: Boolean = false
    }
}
