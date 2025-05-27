package io.kudos.ability.comm.websocket.spring.init

import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import jakarta.annotation.PostConstruct
import org.soul.ability.comm.websocket.common.handler.WebSocketConnector
import org.soul.ability.comm.websocket.common.session.IWebSocketManager
import org.soul.ability.comm.websocket.common.session.distributed.DistributedWebSocketManager
import org.soul.ability.comm.websocket.common.session.local.LocalWebSocketManager
import org.soul.ability.comm.websocket.spring.WebSocketInterceptor
import org.soul.ability.comm.websocket.spring.WsMessageHandler
import org.soul.ability.data.memdb.redis.starter.SoulRedisConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.*
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * spring websocket自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
@PropertySource(
    value = ["classpath:kudos-ability-comm-websocket-spring.yml"],
    factory = YamlPropertySourceFactory::class
)
@Import(
    SoulRedisConfiguration::class,
    WebSocketConnector::class,
    WsMessageHandler::class
)
open class SpringWebSocketAutoConfiguration : WebSocketConfigurer, IComponentInitializer {

    @Autowired
    @Lazy
    private lateinit var wsMessageHandler: WsMessageHandler

    @Value("\${kudos.ability.comm.websocket.path:ws.do}")
    private val wsPath: String? = null

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        //普通websocket的连接地址
        registry.addHandler(wsMessageHandler, wsPath).addInterceptors(WebSocketInterceptor()).setAllowedOrigins("*")
        //sockJS方式连接websocket地址
        registry.addHandler(wsMessageHandler, "/socketJs/$wsPath").addInterceptors(WebSocketInterceptor())
            .withSockJS()
    }


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.comm.websocket", name = ["mode"], havingValue = "local")
    open fun localWebSocketManager(): IWebSocketManager = LocalWebSocketManager()

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.comm.websocket", name = ["mode"], havingValue = "distributed")
    open fun distributedWebSocketManager(): IWebSocketManager = DistributedWebSocketManager()


    @PostConstruct
    override fun init() {
        LoggerFactory.getLogger(this).info("【kudos-ability-comm-websocket-spring】初始化完成.")
    }

}
