package io.kudos.ability.web.ktor.plugins

import io.ktor.server.application.*
import io.ktor.util.*
import io.kudos.ability.web.common.trace.TraceKeys
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextElement
import kotlinx.coroutines.withContext

/**
 * Key used to store the [KudosContext] in Ktor call attributes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
val KudosContextCallKey: AttributeKey<KudosContext> = AttributeKey("KudosContextCall")

/**
 * Returns the [KudosContext] bound to the current [ApplicationCall], or null if absent.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
fun ApplicationCall.kudosContextOrNull(): KudosContext? =
    if (attributes.contains(KudosContextCallKey)) attributes[KudosContextCallKey] else null

/**
 * Returns the [KudosContext] bound to the current [ApplicationCall]; throws if absent.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
fun ApplicationCall.kudosContext(): KudosContext =
    requireNotNull(kudosContextOrNull()) { "KudosContext is absent in ApplicationCall.attributes" }

/**
 * Installs [KudosContext] at the `ApplicationCallPipeline.Setup` stage of Ktor.
 *
 * Difference from the servlet-era `WebContextInitFilter`: the servlet version uses ThreadLocal
 * because request-to-thread is one-to-one; Ktor request handling is a coroutine that may switch
 * dispatchers mid-flight, so a `CoroutineContext.Element` (i.e. [KudosContextElement]) is
 * required. This plugin also writes the context into [ApplicationCall.attributes] so that
 * route handlers can reliably read it via [ApplicationCall.kudosContext] or
 * [ApplicationCall.kudosContextOrNull].
 *
 * The business side customizes how a `KudosContext` is built from an `ApplicationCall` via
 * [Configuration.factory] (the default only sets `traceKey`, resolved from
 * [Configuration.traceKeyHeaders] and validated by [TraceKeys], generating a UUID when nothing usable was
 * supplied).
 *
 * Installation — note that a custom factory replaces the default outright, so it has to resolve the trace key
 * through [TraceKeys.resolve] to keep the validation rather than taking the header on trust:
 * ```kotlin
 * install(KudosContextPlugin) {
 *     factory = { call ->
 *         KudosContext().apply {
 *             traceKey = TraceKeys.resolve(listOf(TraceKeys.TRACE_ID_HEADER)) { call.request.headers[it] }
 *             // other custom fields...
 *         }
 *     }
 * }
 * ```
 *
 * **Scope limitation**: the context is only valid within the current call coroutine and its
 * children. If business code launches **independent** coroutines (e.g. `GlobalScope.launch`,
 * or `launch` without a context), those coroutines cannot see `KudosContext` and must use
 * `withContext(currentCoroutineContext()) { ... }` manually.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class KudosContextPlugin private constructor(
    private val factory: (ApplicationCall) -> KudosContext
) {
    /**
     * Plugin configuration: the business side can override [factory] to control the
     * [KudosContext] construction logic for each request.
     * The default factory only sets `traceKey`; the business side fills in the other fields
     * (user / clientInfo etc.).
     */
    class Configuration {

        /**
         * Request headers consulted for a caller-supplied trace key, in precedence order.
         *
         * Defaults to the public header alone. A deployment that also receives internal service-to-service
         * traffic can append `Consts.RequestHeader.TRACE_KEY` (`_UUID`), the header Feign puts on outgoing
         * calls, to continue those traces rather than starting new ones.
         */
        var traceKeyHeaders: List<String> = listOf(TraceKeys.TRACE_ID_HEADER)

        /** Upper bound on an accepted caller-supplied trace key; longer values are replaced by a generated one. */
        var maxTraceKeyLength: Int = TraceKeys.DEFAULT_MAX_LENGTH

        /**
         * Constructor from [ApplicationCall] to [KudosContext]. By default reads `traceKey` from
         * [traceKeyHeaders], **validating** it before propagating and generating a UUID when nothing usable
         * was supplied. The business side usually extends this factory to inject user / clientInfo etc.
         *
         * The validation is not optional hardening. This factory previously took
         * `headers["X-Trace-Id"]` on trust, and a trace key goes straight into log lines — so a caller could
         * embed CR/LF and forge log entries, or send an unbounded value to inflate any log or metrics backend
         * that indexes by trace id. The Spring MVC runtime already rejected those; accepting them here meant
         * the same request was safe or unsafe depending only on which runtime happened to serve it.
         *
         * **Overriding this factory opts out of the check.** Call [TraceKeys.resolve] from a custom factory to
         * keep it.
         */
        var factory: (ApplicationCall) -> KudosContext = { call ->
            val context = KudosContext()
            context.traceKey = TraceKeys.resolve(traceKeyHeaders, maxTraceKeyLength) { call.request.headers[it] }
            context
        }
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, KudosContextPlugin> {

        override val key = AttributeKey<KudosContextPlugin>("KudosContext")

        /**
         * Intercepts each request at the Setup stage: calls the factory to build the context,
         * writes it to call attributes, then wraps it via [withContext] into
         * [KudosContextElement] and calls `proceed()` to continue through the pipeline.
         */
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): KudosContextPlugin {
            val cfg = Configuration().apply(configure)
            val plugin = KudosContextPlugin(cfg.factory)

            pipeline.intercept(ApplicationCallPipeline.Setup) {
                val ctx = plugin.factory(call)
                call.attributes.put(KudosContextCallKey, ctx)
                withContext(KudosContextElement(ctx)) {
                    proceed()
                }
            }
            return plugin
        }
    }

}
