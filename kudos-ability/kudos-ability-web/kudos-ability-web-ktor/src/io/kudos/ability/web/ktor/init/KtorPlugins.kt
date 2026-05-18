package io.kudos.ability.web.ktor.init

import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

// 命名为 pluginsLog 避开 Application.log 扩展属性（slf4j Logger）的隐式接收者冲突
private val pluginsLog = LogFactory.getLog("io.kudos.ability.web.ktor.init.KtorPlugins")

/**
 * 按 [KtorProperties.plugins] 配置开关，向 Ktor [Application] 安装本模块支持的插件。
 *
 * 默认全部启用；某个插件设 `enabled = false` 后本函数不会 install 之，相应能力不可用。
 *
 * @param ktorProperties Ktor 配置；为 null 时通过 [SpringKit] 从容器拿（适用于运行期）。
 *   测试场景显式传入避免依赖 Spring 容器。
 * @author K
 * @since 1.0.0
 */
fun Application.installPlugins(ktorProperties: KtorProperties? = null) {
    val properties = ktorProperties ?: SpringKit.getBean<KtorProperties>()
    val pluginsProperties = properties.plugins

    // 序列化/反序列化（kotlinx-serialization JSON）
    if (pluginsProperties.contentNegotiation.enabled) {
        install(ContentNegotiation) {
            json()
        }
    }

    // 统一处理状态码与异常：记录详细栈到日志，对外只回吐通用错误信息，避免信息泄漏
    if (pluginsProperties.statusPages.enabled) {
        install(StatusPages) {
            exception<IllegalStateException> { call, cause ->
                pluginsLog.error(cause, "IllegalStateException in ktor pipeline: ${cause.message}")
                call.respond(HttpStatusCode.InternalServerError, "Internal server error")
            }
            exception<Throwable> { call, cause ->
                pluginsLog.error(cause, "Unhandled error in ktor pipeline: ${cause.message}")
                call.respond(HttpStatusCode.InternalServerError, "Internal server error")
            }
        }
    }

    // WebSocket：默认 15s ping/timeout、不限单帧大小、不做 masking（服务端通常不需要）
    if (pluginsProperties.webSocket.enabled) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingPeriod = 15.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
    }

}