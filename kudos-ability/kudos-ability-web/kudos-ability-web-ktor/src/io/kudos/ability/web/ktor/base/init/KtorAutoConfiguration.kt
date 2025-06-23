package io.kudos.ability.web.ktor.base.init

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import jakarta.annotation.PreDestroy
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CompletableFuture


/**
 * Ktor自动配置类
 *
 * 将Ktor整合进SpringBoot
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class KtorAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this)

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.web.ktor")
    open fun ktorProperties() = KtorProperties()

    @Bean
    open fun ktorEngine(ktorProperties: KtorProperties) : EmbeddedServer<*, *>? {
        logger.info("初始化 ktorEngine ...")
        val engineName = ktorProperties.engine.name
        if (engineName.isBlank()) {
            error("kudos.ability.web.ktor.engine.name丢失！")
        }

        if (engineName.lowercase() == "test") {
            logger.info("engineName配置为test, 将使用测试用例中Ktor内置的虚拟内存测试引擎。")
            return null
        } else {
            val engineClassName = when (engineName.lowercase()) {
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
            val port = ktorProperties.engine.port
            val server = embeddedServer(factory, port) {
                KtorContext.application = this
                installPlugins(ktorProperties)
                monitor.subscribe(ApplicationStarted) {
                    logger.info("$engineName 引擎启动成功，port：$port")
                    started.complete(Unit)
                }
            }
            server.start(wait = false)
            started.join()
            return server
        }
    }

    /**
     * Spring 容器关闭前会调用带 @PreDestroy 的方法，
     * 此时调用 Ktor 引擎的 stop 方法优雅关闭。
     */
    @PreDestroy
    open fun shutDownKtor() {
        KtorContext.application.engine.let {
            val engineName = KtorProperties().engine.name
            logger.info(">>> Spring Context 关闭，准备关闭 $engineName 引擎...")
            // gracePeriod = 0 秒，timeout = 1 秒，可以根据需要调整
            it.stop(2000L, 3000L)
            logger.info(">>> $engineName 引擎已停止.")
        }
    }

    override fun getComponentName() = "kudos-ability-web-ktor-base"

}

