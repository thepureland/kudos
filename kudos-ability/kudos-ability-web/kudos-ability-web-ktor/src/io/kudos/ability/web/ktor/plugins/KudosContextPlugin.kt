package io.kudos.ability.web.ktor.plugins

import io.ktor.server.application.*
import io.ktor.util.*
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextElement
import kotlinx.coroutines.withContext
import java.util.UUID

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
 * [Configuration.factory] (the default only sets `traceKey`, read from the `X-Trace-Id` header,
 * generating a UUID when missing).
 *
 * Installation:
 * ```kotlin
 * install(KudosContextPlugin) {
 *     factory = { call ->
 *         KudosContext().apply {
 *             traceKey = call.request.headers["X-Trace-Id"] ?: UUID.randomUUID().toString()
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
         * Constructor from [ApplicationCall] to [KudosContext]. By default reads `traceKey` from
         * `X-Trace-Id`, generating a UUID when missing. The business side usually extends this
         * factory to inject user / clientInfo etc.
         */
        var factory: (ApplicationCall) -> KudosContext = { call ->
            KudosContext().apply {
                traceKey = call.request.headers["X-Trace-Id"] ?: UUID.randomUUID().toString()
            }
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
