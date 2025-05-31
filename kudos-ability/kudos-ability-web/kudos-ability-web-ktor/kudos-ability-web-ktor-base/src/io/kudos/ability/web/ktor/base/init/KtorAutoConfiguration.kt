package io.kudos.ability.web.ktor.base.init

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CompletableFuture


@Configuration
open class KtorAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @Value("\${kudos.ability.web.ktor.engine.name}")
    private var engineName: String? = null

    @Value("\${ktor.deployment.port}")
    private var port : Int? = null

    @Bean
    open fun startKtorEngine() : EmbeddedServer<*, *> {
        logger.info("##### 开始初始化bean startKtorEngine")
        if (engineName.isNullOrBlank()) {
            error("kudos.ability.web.ktor.engine.name丢失！")
        }

        val engineClassName = when (engineName!!.lowercase()) {
            "cio" -> "io.ktor.server.cio.CIO"
            "netty" -> "io.ktor.server.netty.Netty"
            "jetty" -> "io.ktor.server.jetty.jakarta.Jetty"
            "tomcat" -> "io.ktor.server.tomcat.jakarta.Tomcat"
            else -> error("kudos.ability.web.ktor.engine.name值非法！")
        }

        val factory = Class.forName(engineClassName)
            .getField("INSTANCE")
            .get(null) as ApplicationEngineFactory<*, *>

        val started = CompletableFuture<Unit>()
        val server = embeddedServer(factory, port!! ) {
            module()
            routing {
                get("/test") {
                    call.respondText("Hello World!")
                }
            }

            monitor.subscribe(ApplicationStarted) {
                logger.info("$engineName 引擎启动成功，port：$port")
                started.complete(Unit)
            }
        }
        server.start(wait = false)
        started.join()

        logger.info("##### return server")

        return server
    }

    override fun getComponentName() = "kudos-ability-web-ktor-base"

}

fun Application.module() {
    val port = environment.config.propertyOrNull("ktor.environment.port")?.getString()
    println("#######  port: $port")
}