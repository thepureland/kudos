package io.kudos.ability.comm.websocket.common.init

import io.kudos.ability.comm.websocket.common.broadcast.IWebSocketBroadcaster
import io.kudos.ability.comm.websocket.common.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.common.distributed.DistributedWebSocketBroadcaster
import io.kudos.ability.comm.websocket.common.distributed.IWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.common.distributed.redis.RedisWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import kotlin.time.Duration.Companion.milliseconds

/**
 * WebSocket business-layer auto-configuration.
 *
 * Supplies the process-level [KudosWebSocketRegistry] and an [IWebSocketBroadcaster] so applications
 * stop hand-wiring them (and stop each picking a different node-id scheme). Both beans are
 * engine-neutral, so this one configuration serves every engine module — a Ktor application mounts
 * routes with `Route.kudosWebSocket(...)`, a Spring MVC application registers endpoints through
 * `SpringWebSocketAutoConfiguration`, and both autowire the same registry and broadcaster.
 *
 * The broadcaster bean is typed as the [IWebSocketBroadcaster] interface, not the concrete class,
 * so flipping `kudos.ability.comm.websocket.distributed.enabled` to `true` swaps in the
 * cross-process implementation without touching business code.
 *
 * This module installs **no** protocol-level machinery: the Ktor `WebSockets` plugin stays with
 * `kudos-ability-web-ktor`, and the Servlet container's WebSocket settings stay with
 * `kudos-ability-comm-websocket-spring`. Only the business layer is configured here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class WebSocketAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this::class)

    /** Binds `kudos.ability.comm.websocket.*` to [WebSocketProperties]. */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.comm.websocket")
    open fun webSocketProperties() = WebSocketProperties()

    /** Process-level session registry shared by every route and broadcaster. */
    @Bean
    @ConditionalOnMissingBean
    open fun kudosWebSocketRegistry() = KudosWebSocketRegistry()

    /**
     * The broadcaster business code should depend on.
     *
     * Wraps the local broadcaster in [DistributedWebSocketBroadcaster] when a broadcast channel bean
     * is present (see [RedisBroadcastChannelConfiguration]); otherwise stays process-local. The
     * channel is taken as an [ObjectProvider] so this method does not force the Redis configuration
     * to be evaluated — and so a deployment without Redis simply gets local broadcasting instead of
     * a missing-bean failure.
     */
    @Bean
    @ConditionalOnMissingBean(IWebSocketBroadcaster::class)
    open fun webSocketBroadcaster(
        registry: KudosWebSocketRegistry,
        properties: WebSocketProperties,
        channelProvider: ObjectProvider<IWebSocketBroadcastChannel>,
    ): IWebSocketBroadcaster {
        val local = WebSocketBroadcaster(
            registry = registry,
            sendTimeout = properties.broadcast.sendTimeoutMillis.milliseconds,
            maxConcurrentSends = properties.broadcast.maxConcurrentSends,
            closeOnSendTimeout = properties.broadcast.closeOnSendTimeout,
        )
        val channel = channelProvider.ifAvailable
        if (channel == null) {
            logger.info("WebSocket broadcasting is process-local (no IWebSocketBroadcastChannel bean).")
            return local
        }
        val nodeId = properties.nodeId.ifBlank { RandomStringKit.uuid() }
        logger.info("WebSocket broadcasting is distributed via {0}, nodeId={1}", channel::class.simpleName, nodeId)
        return DistributedWebSocketBroadcaster(local = local, channel = channel, nodeId = nodeId)
    }

    override fun getComponentName() = "kudos-ability-comm-websocket-common"

    /**
     * Redis-backed cross-process channel, contributed only when Spring Data Redis is on the
     * classpath (it is `compileOnly` for this module) and the feature is switched on.
     *
     * [ConditionalOnClass] is given the class *name* rather than a class literal on purpose: this
     * configuration is imported by kudos's own component scan, so the annotation may be read
     * reflectively, and a class literal would then need `RedisTemplate` to be loadable just to
     * evaluate the condition that exists to find out whether it is.
     *
     * Requires a [RedisMessageListenerContainer] bean — `kudos-ability-cache-remote-redis` publishes
     * one, and sharing it is intended.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["org.springframework.data.redis.core.RedisTemplate"])
    @ConditionalOnProperty(
        prefix = "kudos.ability.comm.websocket.distributed",
        name = ["enabled"],
        havingValue = "true",
    )
    open class RedisBroadcastChannelConfiguration {

        @Bean
        @ConditionalOnMissingBean(IWebSocketBroadcastChannel::class)
        open fun webSocketBroadcastChannel(
            redisTemplate: RedisTemplate<*, *>,
            container: RedisMessageListenerContainer,
            properties: WebSocketProperties,
        ): IWebSocketBroadcastChannel = RedisWebSocketBroadcastChannel(
            redisTemplate = redisTemplate,
            container = container,
            redisChannel = properties.distributed.redisChannel,
            inboundBufferCapacity = properties.distributed.inboundBufferCapacity,
        )
    }
}
