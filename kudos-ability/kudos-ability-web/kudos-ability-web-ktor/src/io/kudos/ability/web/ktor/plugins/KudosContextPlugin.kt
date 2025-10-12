package io.kudos.ability.web.ktor.plugins

import io.ktor.server.application.*
import io.ktor.util.*
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextElement
import kotlinx.coroutines.withContext
import java.util.*


class KudosContextPlugin private constructor(
    private val factory: (ApplicationCall) -> KudosContext
) {
    class Configuration {
        var factory: (ApplicationCall) -> KudosContext = { call ->
            KudosContext().apply {
                //TODO
                traceKey = call.request.headers["X-Trace-Id"] ?: UUID.randomUUID().toString()
            }
        }
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, KudosContextPlugin> {

        override val key = AttributeKey<KudosContextPlugin>("KudosContext")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): KudosContextPlugin {
            val cfg = Configuration().apply(configure)
            val plugin = KudosContextPlugin(cfg.factory)

            // 在 Setup 阶段拦截 + withContext + proceed()
            pipeline.intercept(ApplicationCallPipeline.Setup) {
                val ctx = plugin.factory(call)
                val cc = KudosContextElement(ctx)
                withContext(cc) {
                    proceed()
                }
            }
            return plugin
        }
    }

}
