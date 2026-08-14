package io.kudos.ability.comm.websocket.ktor.init

import io.kudos.ability.comm.websocket.ktor.broadcast.IWebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.DistributedWebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.IWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.ktor.distributed.redis.RedisWebSocketBroadcastChannel
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
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
 * stop hand-wiring them (and stop each picking a different node-id scheme). Business code then
 * autowires both and mounts routes from an
 * [io.kudos.ability.web.ktor.core.IKtorRouteRegistrar]-style registrar:
 *
 * ```kotlin
 * routing { kudosWebSocket("/ws/chat", registry, handler) { raw -> ... } }
 * ```
 *
 * The broadcaster bean is typed as the [IWebSocketBroadcaster] interface, not the concrete class,
 * so flipping `kudos.ability.comm.websocket.distributed.enabled` to `true` swaps in the
 * cross-process implementation without touching business code.
 *
 * This module does **not** install the Ktor `WebSockets` plugin — that stays with
 * `kudos-ability-web-ktor`, which owns protocol-level settings.
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

    override fun getComponentName() = "kudos-ability-comm-websocket-ktor"

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
