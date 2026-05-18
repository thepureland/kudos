package io.kudos.ability.web.ktor.plugins

import io.ktor.server.application.*
import io.ktor.util.*
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextElement
import kotlinx.coroutines.withContext
import java.util.UUID


/**
 * 把 [KudosContext] 装到 Ktor `ApplicationCallPipeline.Setup` 阶段的协程上下文中。
 *
 * 与 servlet 版 `WebContextInitFilter` 的区别：servlet 用 ThreadLocal，因为请求-线程
 * 一一对应；Ktor 的请求处理是协程，途中可能被 dispatcher 切换线程，所以必须用
 * `CoroutineContext.Element`（即 [KudosContextElement]），由 `withContext` 装好后再
 * `proceed()`，下游 handler 才能通过 `coroutineContext[KudosContextElement]` 拿到上下文。
 *
 * 业务侧通过 [Configuration.factory] 自定义如何从 `ApplicationCall` 构造 `KudosContext`
 * （默认仅设置 `traceKey`，从 `X-Trace-Id` header 取，缺失则生成 UUID）。
 *
 * 安装：
 * ```kotlin
 * install(KudosContextPlugin) {
 *     factory = { call ->
 *         KudosContext().apply {
 *             traceKey = call.request.headers["X-Trace-Id"] ?: UUID.randomUUID().toString()
 *             // 其他自定义字段...
 *         }
 *     }
 * }
 * ```
 *
 * **范围限制**：context 只在当前调用协程及其子协程内有效；
 * 业务代码若启动**独立**协程（如 `GlobalScope.launch`、未带 context 的 `launch`），
 * 这些协程拿不到 `KudosContext`，需手动 `withContext(currentCoroutineContext()) { ... }`。
 *
 * **已知问题**：当前用 `pipeline.intercept(Setup) { withContext(...) { proceed() } }`
 * 在 Ktor 路由 handler 内通过 `coroutineContext[KudosContextElement]` 取不到注入的 element
 * （routing 子管线 dispatch 时未沿用 Setup 阶段的 coroutine context）。
 * 修复方向：改用 `call.attributes.put(KudosContextKey, ctx)` 或注册到自定义 `RouteScopedPlugin`。
 * 该问题已在 [KudosContextPluginTest] 头注释中记录；目前本插件实际用途仅"占位"。
 *
 * @author K
 * @since 1.0.0
 */
class KudosContextPlugin private constructor(
    private val factory: (ApplicationCall) -> KudosContext
) {
    /**
     * 插件配置：业务可覆盖 [factory] 决定每个请求的 [KudosContext] 构造逻辑。
     * 默认 factory 仅设置 `traceKey`，其他字段（user / clientInfo 等）由业务侧补全。
     */
    class Configuration {
        /**
         * 由 [ApplicationCall] → [KudosContext] 的构造函数。默认从 `X-Trace-Id` 取 traceKey，
         * 缺失则生成 UUID。业务侧通常会扩展此 factory 注入 user / clientInfo 等字段。
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
         * 在 Setup 阶段拦截每个请求：调用 factory 构造 context、用 [withContext] 套入
         * [KudosContextElement]、`proceed()` 走完后续 pipeline。
         */
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): KudosContextPlugin {
            val cfg = Configuration().apply(configure)
            val plugin = KudosContextPlugin(cfg.factory)

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
