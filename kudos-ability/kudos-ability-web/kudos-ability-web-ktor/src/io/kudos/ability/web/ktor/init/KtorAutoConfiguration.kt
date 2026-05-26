package io.kudos.ability.web.ktor.init

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.kudos.ability.web.ktor.core.IKtorRouteRegistrar
import io.kudos.ability.web.ktor.core.KtorContext
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CompletableFuture


/**
 * Ktor auto-configuration class.
 *
 * Integrates Ktor into Spring Boot.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class KtorAutoConfiguration : IComponentInitializer {

    /** All [IKtorRouteRegistrar] implementations collected by Spring, used to register routes uniformly when Ktor starts. */
    @Autowired(required = false)
    private var routeRegistrar: List<IKtorRouteRegistrar> = emptyList()

    /** Binds `kudos.ability.web.ktor.*` to [KtorProperties]. */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.web.ktor")
    open fun ktorProperties() = KtorProperties()

    /**
     * Starts the embedded Ktor engine. Flow:
     *  1. Stash the configuration in [KtorContext.properties] for downstream use.
     *  2. When `engine.name = test`, return null directly (tests wire up their own testApplication).
     *  3. Otherwise reflectively load the corresponding engine factory `INSTANCE` (so all four
     *     engines do not need to be pulled into the classpath at compile time).
     *  4. `embeddedServer { installPlugins(...); routing { routeRegistrar.forEach(register) } }`.
     *  5. `monitor.subscribe(ApplicationStarted)` with a [CompletableFuture] to wait synchronously
     *     for startup to complete, so that we do not return during Spring container init while
     *     Ktor is not yet truly ready.
     */
    @Bean
    open fun ktorEngine(ktorProperties: KtorProperties): EmbeddedServer<*, *>? {
        logger.info("Initializing ktorEngine ...")
        KtorContext.properties = ktorProperties
        val engineName = ktorProperties.engine.name
        require(engineName.isNotBlank()) { "kudos.ability.web.ktor.engine.name is missing!" }

        if (engineName.lowercase() == "test") {
            logger.info("engineName is configured as test; the in-memory test engine built into Ktor will be used in test cases.")
            return null
        }

        val engineClassName = when (engineName.lowercase()) {
            "cio" -> "io.ktor.server.cio.CIO"
            "netty" -> "io.ktor.server.netty.Netty"
            "jetty" -> "io.ktor.server.jetty.jakarta.Jetty"
            "tomcat" -> "io.ktor.server.tomcat.jakarta.Tomcat"
            else -> error("kudos.ability.web.ktor.engine.name has an invalid value!")
        }

        val factory = Class.forName(engineClassName)
            .getField("INSTANCE")
            .get(null) as ApplicationEngineFactory<*, *>

        val started = CompletableFuture<Unit>()
        val port = ktorProperties.engine.port

        val server = embeddedServer(factory, port) {
            KtorContext.application = this
            installPlugins(ktorProperties)
            routing {
                routeRegistrar.forEach { it.register(this) }
            }
            monitor.subscribe(ApplicationStarted) {
                logger.info("$engineName engine started successfully, port: $port")
                started.complete(Unit)
            }
        }
        server.start(wait = false)
        started.join()
        return server
    }

    /**
     * `@PreDestroy` hook before the Spring container shuts down: gracefully stops the Ktor engine.
     *
     * `stop(2000L, 3000L)` meaning: give in-flight requests 2 seconds to finish, then force-wait
     * 3 more seconds before fully shutting down. If the application is not initialized (e.g.
     * engine.name=test returned null), skip directly to avoid NPE.
     *
     * @author K
     * @since 1.0.0
     */
    @PreDestroy
    open fun shutDownKtor() {
        if (!KtorContext.isApplicationInitialized()) {
            logger.debug("Ktor Application not initialized; skipping engine shutdown.")
            return
        }
        val engineName = KtorContext.properties.engine.name
        logger.info(">>> Spring Context shutting down; preparing to stop the $engineName engine...")
        KtorContext.application.engine.stop(2000L, 3000L)
        logger.info(">>> $engineName engine stopped.")
    }

    /**
     * Implements the [IComponentInitializer] contract: returns this component's name, used by
     * ComponentInitializationDispatcher for ordering and log identification.
     *
     * @return component name constant
     * @author K
     * @since 1.0.0
     */
    override fun getComponentName() = "kudos-ability-web-ktor-base"

    /** Logger. */
    private val logger = LogFactory.getLog(this::class)

}

