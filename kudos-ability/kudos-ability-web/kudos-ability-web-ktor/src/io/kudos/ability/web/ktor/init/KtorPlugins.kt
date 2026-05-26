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

/**
 * Logger used during Ktor plugin installation.
 *
 * Deliberately not named `log` — Ktor's [io.ktor.server.application.Application.log]
 * is an extension property; using `log` inside the `Application` receiver scope would
 * be captured by it, leading to the ambiguity of "we think we're writing to LogFactory
 * but it's actually slf4j". Explicitly naming it `pluginsLog` keeps the two apart.
 */
// Named pluginsLog to avoid an implicit-receiver clash with the Application.log extension property (slf4j Logger).
private val pluginsLog = LogFactory.getLog("io.kudos.ability.web.ktor.init.KtorPlugins")

/**
 * Installs the plugins supported by this module into the Ktor [Application] per
 * the [KtorProperties.plugins] switches.
 *
 * All enabled by default; once a plugin is set to `enabled = false`, this function
 * will not install it and the corresponding capability is unavailable.
 *
 * @param ktorProperties Ktor configuration; when null, fetched from the Spring
 *   container via [SpringKit] (for runtime use). Tests pass it explicitly to avoid
 *   depending on the Spring container.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
fun Application.installPlugins(ktorProperties: KtorProperties? = null) {
    val properties = ktorProperties ?: SpringKit.getBean<KtorProperties>()
    val pluginsProperties = properties.plugins

    // Serialization / deserialization (kotlinx-serialization JSON)
    if (pluginsProperties.contentNegotiation.enabled) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Unified status code / exception handling: log the full stack trace, expose only a generic error to clients, avoiding information leakage.
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

    // WebSocket: defaults to 15s ping/timeout, no per-frame size limit, no masking (servers typically do not need it).
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