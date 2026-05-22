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
 * Ktor自动配置类
 *
 * 将Ktor整合进SpringBoot
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class KtorAutoConfiguration : IComponentInitializer {

    /** 由 Spring 收集的所有 [IKtorRouteRegistrar] 实现，用于在 Ktor 启动时统一注册路由 */
    @Autowired(required = false)
    private var routeRegistrar: List<IKtorRouteRegistrar> = emptyList()

    /** 绑定 `kudos.ability.web.ktor.*` 到 [KtorProperties]。 */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.web.ktor")
    open fun ktorProperties() = KtorProperties()

    /**
     * 启动 Ktor 内嵌引擎。流程：
     *  1. 把配置塞到 [KtorContext.properties]，供下游引用
     *  2. `engine.name = test` 时直接返回 null（由测试自行装配 testApplication）
     *  3. 否则反射加载对应引擎工厂的 `INSTANCE`（避免编译期把 4 套引擎都拉进 classpath）
     *  4. `embeddedServer { installPlugins(...); routing { routeRegistrar.forEach(register) } }`
     *  5. `monitor.subscribe(ApplicationStarted)` 配合 [CompletableFuture] 同步等启动完成，
     *     避免 Spring 容器初始化阶段就返回但 Ktor 还没真正就绪
     */
    @Bean
    open fun ktorEngine(ktorProperties: KtorProperties): EmbeddedServer<*, *>? {
        logger.info("初始化 ktorEngine ...")
        KtorContext.properties = ktorProperties
        val engineName = ktorProperties.engine.name
        require(engineName.isNotBlank()) { "kudos.ability.web.ktor.engine.name丢失！" }

        if (engineName.lowercase() == "test") {
            logger.info("engineName配置为test, 将使用测试用例中Ktor内置的虚拟内存测试引擎。")
            return null
        }

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
            routing {
                routeRegistrar.forEach { it.register(this) }
            }
            monitor.subscribe(ApplicationStarted) {
                logger.info("$engineName 引擎启动成功，port：$port")
                started.complete(Unit)
            }
        }
        server.start(wait = false)
        started.join()
        return server
    }

    /**
     * Spring 容器关闭前 `@PreDestroy` 钩子：优雅关闭 Ktor 引擎。
     *
     * `stop(2000L, 3000L)` 含义：先给在途请求 2 秒处理完，再强制等 3 秒后彻底关；
     * 未初始化 application（如 engine.name=test 时直接返回 null）就直接跳过，避免空指针。
     *
     * @author K
     * @since 1.0.0
     */
    @PreDestroy
    open fun shutDownKtor() {
        if (!KtorContext.isApplicationInitialized()) {
            logger.debug("Ktor Application 未初始化，跳过引擎关闭")
            return
        }
        val engineName = KtorContext.properties.engine.name
        logger.info(">>> Spring Context 关闭，准备关闭 $engineName 引擎...")
        KtorContext.application.engine.stop(2000L, 3000L)
        logger.info(">>> $engineName 引擎已停止.")
    }

    /**
     * 实现 [IComponentInitializer] 契约：返回当前组件的名字，供 ComponentInitializationDispatcher 排序与日志识别。
     *
     * @return 组件名常量
     * @author K
     * @since 1.0.0
     */
    override fun getComponentName() = "kudos-ability-web-ktor-base"

    /** 日志器 */
    private val logger = LogFactory.getLog(this::class)

}

