package io.kudos.ability.web.ktor.init

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * test for KtorPlugins (installPlugins 扩展函数)
 *
 * 覆盖：
 * 1. 默认配置下三个插件(ContentNegotiation/StatusPages/WebSockets)均安装
 * 2. 全部 enabled=false 时均不安装
 * 3. StatusPages 的 IllegalStateException 专用处理器：500 + 通用错误消息（不泄漏内部信息）
 * 4. StatusPages 的 Throwable 兜底处理器：500 + 通用错误消息
 *
 * @author K
 * @since 1.0.0
 */
internal class KtorPluginsTest {

    @Test
    fun installPlugins_allEnabledByDefault() = testApplication {
        application {
            installPlugins(KtorProperties())
            assertNotNull(pluginOrNull(ContentNegotiation))
            assertNotNull(pluginOrNull(StatusPages))
            assertNotNull(pluginOrNull(WebSockets))
        }
        startApplication()
    }

    @Test
    fun installPlugins_allDisabled_installsNothing() = testApplication {
        val properties = KtorProperties(
            plugins = KtorProperties.Plugins(
                contentNegotiation = KtorProperties.Plugins.Plugin(enabled = false),
                statusPages = KtorProperties.Plugins.Plugin(enabled = false),
                webSocket = KtorProperties.Plugins.Plugin(enabled = false)
            )
        )
        application {
            installPlugins(properties)
            assertNull(pluginOrNull(ContentNegotiation))
            assertNull(pluginOrNull(StatusPages))
            assertNull(pluginOrNull(WebSockets))
        }
        startApplication()
    }

    @Test
    fun statusPages_illegalStateException_respondsGenericInternalError() = testApplication {
        application {
            installPlugins(KtorProperties())
            routing {
                get("/ise") {
                    throw IllegalStateException("sensitive internal detail")
                }
            }
        }

        val response = client.get("/ise")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        // 只返回通用错误，不能把内部异常信息泄漏给客户端
        assertEquals("Internal server error", response.bodyAsText())
    }

    @Test
    fun statusPages_otherThrowable_respondsGenericInternalError() = testApplication {
        application {
            installPlugins(KtorProperties())
            routing {
                get("/boom") {
                    throw UnsupportedOperationException("another sensitive detail")
                }
            }
        }

        val response = client.get("/boom")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Internal server error", response.bodyAsText())
    }

}
