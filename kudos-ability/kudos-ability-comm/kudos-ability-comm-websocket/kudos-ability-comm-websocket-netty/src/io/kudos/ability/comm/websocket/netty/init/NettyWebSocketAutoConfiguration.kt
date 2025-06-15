package io.kudos.ability.comm.websocket.netty.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import org.soul.ability.comm.websocket.common.api.IWebsocketSenderAPI
import org.soul.ability.comm.websocket.common.handler.DefaultWebsocketSender
import org.soul.ability.comm.websocket.common.handler.WebSocketConnector
import org.soul.ability.comm.websocket.common.session.IWebSocketManager
import org.soul.ability.comm.websocket.common.session.distributed.DistributedWebSocketManager
import org.soul.ability.comm.websocket.common.session.local.LocalWebSocketManager
import org.soul.ability.comm.websocket.netty.NettyWsHeartBeatHandler
import org.soul.ability.comm.websocket.netty.WebsocketNettyHandler
import org.soul.ability.comm.websocket.netty.WebsocketNettyServer
import org.soul.ability.comm.websocket.netty.starter.properties.NettyWebsocketProperties
import org.soul.ability.data.memdb.redis.starter.SoulRedisConfiguration
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource


/**
 * netty websocket自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configurable
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-comm-websocket-netty.yml"],
    factory = YamlPropertySourceFactory::class
)
@Import(
    SoulRedisConfiguration::class,
    WebSocketConnector::class,
    NettyWsHeartBeatHandler::class,
    WebsocketNettyHandler::class,
    WebsocketNettyServer::class
)
open class NettyWebSocketAutoConfiguration : IComponentInitializer {

    @Bean(IWebsocketSenderAPI.BEAN_NAME)
    @ConditionalOnMissingBean
    open fun websocketSenderAPI(): IWebsocketSenderAPI = DefaultWebsocketSender()

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.comm.websocket", name = ["mode"], havingValue = "local")
    open fun localWebSocketManager(): IWebSocketManager = LocalWebSocketManager()

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.comm.websocket", name = ["mode"], havingValue = "distributed")
    open fun distributedWebSocketManager(): IWebSocketManager = DistributedWebSocketManager()


    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.comm.websocket.netty")
    open fun nettyWebsocketProperties() = NettyWebsocketProperties()

    override fun getComponentName() = "kudos-ability-comm-websocket-netty"

}