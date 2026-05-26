package io.kudos.ability.distributed.stream.rabbit.init

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
 * RabbitMQ stream broker integration configuration.
 *
 * Extends [StreamCommonConfiguration] — all the producer / consumer / failure-retry / binding
 * verifier beans actually come from the parent. This class only handles:
 * 1. Using [PropertySource] + [YamlPropertySourceFactory] to merge `kudos-ability-distributed-stream-rabbit.yml`
 *    and `kudos-ability-distributed-stream-common.yml` into the Spring Environment.
 * 2. Letting the kudos custom-configuration SPI recognize this module via [IComponentInitializer].
 *
 * [AutoConfigureAfter] is honored under the kudos framework — `ComponentInitializationDispatcher`
 * schedules IComponentInitializer instances in dependency order, so this class is guaranteed to
 * initialize after [ContextAutoConfiguration].
 *
 * Enables multi-binding function.definition auto-aggregation via [StreamConsumerEnvironRegistrar],
 * so consumers declared across multiple kudos ymls can be merged and registered.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = [
        "classpath:kudos-ability-distributed-stream-common.yml",
        "classpath:kudos-ability-distributed-stream-rabbit.yml"
    ],
    factory = YamlPropertySourceFactory::class
)
@Import(StreamConsumerEnvironRegistrar::class)
open class RabbitMqAutoConfiguration : StreamCommonConfiguration(), IComponentInitializer {

    /** Component name for the kudos configuration SPI — must be unique across all modules; conventionally matches the jar artifact name. */
    override fun getComponentName() = "kudos-ability-distributed-stream-rabbit"

}
