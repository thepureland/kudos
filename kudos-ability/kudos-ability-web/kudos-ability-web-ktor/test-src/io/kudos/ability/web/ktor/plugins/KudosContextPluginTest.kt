package io.kudos.ability.web.ktor.plugins

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.kudos.context.core.KudosContext
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [KudosContextPlugin] 安装路径的烟雾测试。
 *
 * **已知限制（未解决）**：插件用
 * `pipeline.intercept(ApplicationCallPipeline.Setup) { withContext(KudosContextElement(...)) { proceed() } }`
 * 注入 `KudosContextElement`，但 Ktor 当前版本的 routing 子管线在分发到 handler 时似乎不会
 * 沿用 Setup 阶段 `withContext` 所设的 element，导致 handler 内 `coroutineContext[KudosContextElement]`
 * 取出来是 null。
 *
 * 因此本测试只验证：
 *  - 插件安装时不抛错
 *  - 自定义 [KudosContextPlugin.Configuration.factory] 能被执行（用 side-effect 反推）
 *  - 请求最终成功返回
 *
 * 深度的"handler 拿到 KudosContext"验证目前留作 README 中已知问题项。
 */
class KudosContextPluginTest {

    @Test
    fun pluginInstalls_andHandlerStillRuns() = testApplication {
        application {
            install(KudosContextPlugin)
            routing {
                get("/") { call.respondText("hello") }
            }
        }

        val response = client.get("/") { header("X-Trace-Id", "trace-abc") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("hello", response.bodyAsText())
    }

    @Test
    fun customFactory_isInvokedPerCall() = testApplication {
        var factoryCalls = 0
        application {
            install(KudosContextPlugin) {
                factory = { _ ->
                    factoryCalls++
                    KudosContext().apply { traceKey = "fixed-key" }
                }
            }
            routing {
                get("/") { call.respondText("ok") }
            }
        }

        client.get("/")
        client.get("/")
        assertEquals(2, factoryCalls)
    }
}
