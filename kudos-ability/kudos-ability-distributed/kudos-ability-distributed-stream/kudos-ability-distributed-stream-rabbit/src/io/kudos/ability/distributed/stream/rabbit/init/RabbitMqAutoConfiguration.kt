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
 * RabbitMQ stream broker 接入装配。
 *
 * 继承 [StreamCommonConfiguration] —— 实际所有 producer / consumer / 失败重试 / binding
 * 校验的 bean 都来自父类。本类只负责：
 * 1. 用 [PropertySource] + [YamlPropertySourceFactory] 把 `kudos-ability-distributed-stream-rabbit.yml`
 *    与 `kudos-ability-distributed-stream-common.yml` 合并到 Spring Environment
 * 2. 通过 [IComponentInitializer] 让 kudos 自定义装配 SPI 识别本模块
 *
 * [AutoConfigureAfter] 在 kudos 体系下有效——`ComponentInitializationDispatcher` 会按依赖
 * 顺序调度 IComponentInitializer 实例，所以本类一定在 [ContextAutoConfiguration] 之后初始化。
 *
 * 通过 [StreamConsumerEnvironRegistrar] 启用 multi-binding function.definition 自动聚合，
 * 让多个 kudos yml 中声明的 consumer 能合并注册。
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

    /** kudos 装配 SPI 用的组件名——务必全模块唯一，与 jar artifact 同名约定。 */
    override fun getComponentName() = "kudos-ability-distributed-stream-rabbit"

}
