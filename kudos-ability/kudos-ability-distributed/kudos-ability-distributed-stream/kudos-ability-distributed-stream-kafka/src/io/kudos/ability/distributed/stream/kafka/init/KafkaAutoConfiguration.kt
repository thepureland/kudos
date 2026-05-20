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
 * Kafka stream broker 接入装配。
 *
 * 继承 [StreamCommonConfiguration]——producer / consumer / 失败重试 / binding 校验的 bean
 * 都从父类来，本类只做两件事：
 * 1. 合并 `kudos-ability-distributed-stream-common.yml` + `kudos-ability-distributed-stream-kafka.yml`
 *    到 Spring Environment
 * 2. 通过 [IComponentInitializer] 让 kudos 自定义装配 SPI 在 [ContextAutoConfiguration] 之后
 *    调度本模块
 *
 * 通过 [StreamConsumerEnvironRegistrar] 启用 multi-binding function.definition 自动聚合。
 *
 * **kafka 特有注意**：spring-cloud-stream-kafka 默认会把 kafka_* header 透传到 consumer，
 * `StreamGlobalExceptionHandler.isFromConsumer` 据此判断来源。如要透传自定义 header，配置：
 * `spring.cloud.stream.kafka.binder.headers: HEADER_A,HEADER_B`。
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

    /** kudos 装配 SPI 用的组件名——务必全模块唯一，与 jar artifact 同名约定。 */
    override fun getComponentName() = "kudos-ability-distributed-stream-kafka"

}
