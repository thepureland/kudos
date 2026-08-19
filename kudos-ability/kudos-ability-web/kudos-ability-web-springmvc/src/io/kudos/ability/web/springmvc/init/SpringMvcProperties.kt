package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.common.trace.TraceKeys
import io.kudos.ability.web.springmvc.support.enums.ServletServerEnum
import io.kudos.context.support.Consts
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for this module, bound from the `kudos.ability.web.springmvc` prefix.
 *
 * Replaces the scattered `env.getProperty("kudos.ability.web.springmvc.xxx")` string lookups: with a
 * bound properties class the keys are discoverable in the IDE, type-converted once, and validated at
 * startup rather than silently defaulting on a typo.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.web.springmvc")
open class SpringMvcProperties {

    /**
     * Which embedded servlet container to force.
     *
     * **Leave unset in production.** When null, no container factory bean is contributed and Spring Boot
     * selects the container from the classpath — its native path, where every `server.*` property and every
     * container-specific customizer applies without this module standing in the way.
     *
     * Set it only when several container dependencies coexist and one must win deterministically (this
     * module's own Tomcat/Jetty test matrix is exactly that case).
     */
    var server: ServletServerEnum? = null

    /** CORS defaults applied by [SpringMvcAutoConfiguration.addCorsMappings]. */
    var cors: Cors = Cors()

    /** Tomcat-specific tweaks applied through a `TomcatConnectorCustomizer` bean. */
    var tomcat: Tomcat = Tomcat()

    /** Request-context / tracing behaviour of [io.kudos.ability.web.springmvc.filter.WebContextInitFilter]. */
    var context: Context = Context()

    /**
     * CORS settings.
     *
     * The defaults are deliberately permissive so that CORS never blocks local development. They are
     * **not** safe for production: `allowedOriginPatterns = ["*"]` together with `allowCredentials = true`
     * means "reflect whatever Origin the caller sends, and let it carry cookies", which is an open door.
     * [SpringMvcAutoConfiguration] logs a startup warning whenever that combination is still active, so the
     * risk is visible in the logs of any environment that forgot to tighten it.
     */
    open class Cors {

        /** Whether to register the CORS mapping at all. Set false to hand CORS entirely to a gateway or a custom configurer. */
        var enabled: Boolean = true

        /** Path pattern the CORS mapping is registered for. */
        var pathPattern: String = "/**"

        /**
         * Exact allowed origins. When non-empty this takes precedence and [allowedOriginPatterns] is ignored,
         * so tightening only requires setting this one key rather than also having to clear the pattern default.
         */
        var allowedOrigins: List<String> = emptyList()

        /** Allowed origin patterns; used only when [allowedOrigins] is empty. */
        var allowedOriginPatterns: List<String> = listOf("*")

        /** Allowed HTTP methods. */
        var allowedMethods: List<String> = listOf("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD")

        /** Allowed request headers. */
        var allowedHeaders: List<String> = listOf("*")

        /** Response headers exposed to the browser. */
        var exposedHeaders: List<String> = emptyList()

        /** Whether the browser may send credentials (cookies / Authorization) cross-origin. */
        var allowCredentials: Boolean = true

        /** Preflight cache lifetime, in seconds. */
        var maxAge: Long = 24 * 60 * 60

    }

    /** Tomcat connector settings. */
    open class Tomcat {

        /**
         * Characters allowed verbatim in the request path. Tomcat rejects `"<>[\]^`{|}` with a bare 400 by
         * default, which surprises callers whose URLs legitimately contain them.
         */
        var relaxedPathChars: String = "\"<>[\\]^`{|}"

        /** Same as [relaxedPathChars], for the query string. */
        var relaxedQueryChars: String = "\"<>[\\]^`{|}"

    }

    /** Request-context settings. */
    open class Context {

        /**
         * The public trace header: both the response header the resolved trace key is echoed back on, **and**
         * the first request header consulted when resolving it.
         *
         * Serving both directions from one setting is deliberate. A caller that reads a trace id off the
         * response and sends it back on the next request must be able to use the same header name; splitting
         * the two into independent settings is what allowed them to drift apart in the first place, leaving the
         * response advertising a name the request side did not accept.
         *
         * Echoing matters because the trace id otherwise reaches the caller only inside a wrapped `ApiResponse`
         * body — missing from exactly the responses that need it most: non-JSON payloads, endpoints annotated
         * `@IgnoreApiResponseWrap`, and container-level errors that short-circuit before any advice runs.
         *
         * Set to an empty string to opt out of the public header entirely: nothing is echoed, and only
         * [additionalTraceKeyHeaders] is consulted on the way in.
         */
        var traceIdHeader: String = TraceKeys.TRACE_ID_HEADER

        /**
         * Further request headers consulted for a caller-supplied trace key, in order, **after**
         * [traceIdHeader].
         *
         * Defaults to `_UUID` ([io.kudos.context.support.Consts.RequestHeader.TRACE_KEY]), the internal
         * cross-process header that `GlobalHeaderRequestInterceptor` puts on outgoing Feign calls and that
         * `InternalRpcContextSignatureVerifier` folds into its request signature. Every internal service-to-service
         * call therefore keeps resolving exactly as before; the public header simply extends the same
         * capability to external callers, who cannot be expected to speak an underscore-prefixed internal name.
         *
         * [traceIdHeader] deliberately wins when both are present: honouring the name the response advertises
         * is the rule a caller can predict, and it keeps the two directions symmetric no matter how the header
         * is renamed.
         */
        var additionalTraceKeyHeaders: List<String> = listOf(Consts.RequestHeader.TRACE_KEY)

        /**
         * Maximum accepted length of a caller-supplied trace key. Longer values are replaced by a freshly
         * generated one.
         */
        var maxTraceKeyLength: Int = 128

        /**
         * Ant-style paths the context filter skips entirely. Static resources and health probes gain nothing
         * from a full session/header/cookie copy and pay for it on every request.
         */
        var excludePathPatterns: List<String> = emptyList()

    }

}
