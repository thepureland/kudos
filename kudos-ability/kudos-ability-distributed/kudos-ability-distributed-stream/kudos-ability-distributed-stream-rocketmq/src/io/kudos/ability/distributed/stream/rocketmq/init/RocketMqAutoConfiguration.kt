package io.kudos.ability.distributed.stream.rocketmq.init

import io.kudos.ability.distributed.stream.common.init.StreamCommonConfiguration
import io.kudos.ability.distributed.stream.common.init.StreamConsumerEnvironRegistrar
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource


/**
 * RocketMQ stream broker auto-configuration.
 *
 * Extends [StreamCommonConfiguration] so `@MqProducer`, failure retries, and binding validation all
 * share the parent's beans. Same shape as the rabbit / kafka AutoConfiguration; this module
 * additionally pulls in:
 * - [io.kudos.ability.distributed.stream.rocketmq.init.properties.RocketMqProperties] — exposes
 *   `nameSrvAddr` and `saveException` for [RocketMqBatchConsumer].
 * - [io.kudos.ability.distributed.stream.rocketmq.support.RocketMqBatchConsumer] — a batch-pull
 *   consumer wrapping the native `DefaultLitePullConsumer` (a separate path alongside the stream
 *   consumer, useful when the business needs batch-size + commit-timing control).
 *
 * [AutoConfigureAfter] is honored in the kudos system: `ComponentInitializationDispatcher`
 * schedules in dependency order, and this module is initialized after [ContextAutoConfiguration].
 *
 * [StreamConsumerEnvironRegistrar] enables automatic multi-binding function.definition aggregation.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = [
        "classpath:kudos-ability-distributed-stream-common.yml",
        "classpath:kudos-ability-distributed-stream-rocketmq.yml"
    ],
    factory = YamlPropertySourceFactory::class
)
@Import(StreamConsumerEnvironRegistrar::class)
open class RocketMqAutoConfiguration : StreamCommonConfiguration(), IComponentInitializer {

    /** Component name used by the kudos auto-configuration SPI — must be unique across modules; by convention matches the jar artifact name. */
    override fun getComponentName() = "kudos-ability-distributed-stream-rocketmq"

}
