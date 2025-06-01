package io.kudos.ability.web.ktor.base.init

import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.kudos.context.kit.SpringKit
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * 向Ktor的application注册插件
 *
 * @param ktorProperties Ktor配置属性
 * @author K
 * @since 1.0.0
 */
fun Application.installPlugins(ktorProperties: KtorProperties? = null) {
    val properties = ktorProperties ?: SpringKit.getBean(KtorProperties::class)
    val pluginsProperties = properties.plugins

    // 序列化/反序列化
    if (pluginsProperties.contentNegotiation.enabled) {
        install(ContentNegotiation) {
            json()
        }
    }

    // 统一处理状态码与异常
    if (pluginsProperties.statusPages.enabled) {
        install(StatusPages) {
            exception<IllegalStateException> { call, cause ->
                call.respondText("App in illegal state as ${cause.message}")
            }
        }
    }

    // WebSocket
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