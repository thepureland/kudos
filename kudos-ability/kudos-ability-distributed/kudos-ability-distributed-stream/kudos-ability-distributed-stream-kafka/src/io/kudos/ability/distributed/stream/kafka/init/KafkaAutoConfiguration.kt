package io.kudos.ability.distributed.stream.kafka.init

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
 * Auto-configuration that wires up the Kafka stream broker.
 *
 * Extends [StreamCommonConfiguration] — producer / consumer / failure-retry / binding-validation
 * beans all come from the parent; this class only does two things:
 * 1. Merge `kudos-ability-distributed-stream-common.yml` + `kudos-ability-distributed-stream-kafka.yml`
 *    into the Spring Environment
 * 2. Through [IComponentInitializer], let the kudos custom wiring SPI schedule this module after
 *    [ContextAutoConfiguration]
 *
 * Enables multi-binding `function.definition` auto-aggregation via [StreamConsumerEnvironRegistrar].
 *
 * **Kafka-specific note**: spring-cloud-stream-kafka propagates `kafka_*` headers to the consumer
 * by default, which `StreamGlobalExceptionHandler.isFromConsumer` uses to detect the source. To
 * propagate custom headers, configure: `spring.cloud.stream.kafka.binder.headers: HEADER_A,HEADER_B`.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = [
        "classpath:kudos-ability-distributed-stream-common.yml",
        "classpath:kudos-ability-distributed-stream-kafka.yml"
    ],
    factory = YamlPropertySourceFactory::class
)
@Import(StreamConsumerEnvironRegistrar::class)
open class KafkaAutoConfiguration : StreamCommonConfiguration(), IComponentInitializer {

    /** Component name used by the kudos wiring SPI — must be globally unique across modules and matches the jar artifact name by convention. */
    override fun getComponentName() = "kudos-ability-distributed-stream-kafka"

}
