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
 * RocketMQ stream broker 接入装配。
 *
 * 继承 [StreamCommonConfiguration]——`@MqProducer` / 失败重试 / binding 校验全部走父类 bean。
 * 跟 rabbit / kafka 的 AutoConfiguration 同形态；本模块额外引入：
 * - [io.kudos.ability.distributed.stream.rocketmq.init.properties.RocketMqProperties]——
 *   暴露 `nameSrvAddr` + `saveException` 给 [RocketMqBatchConsumer] 用
 * - [io.kudos.ability.distributed.stream.rocketmq.support.RocketMqBatchConsumer]——
 *   原生 `DefaultLitePullConsumer` 包装的批量拉取消费者（与 stream consumer 并行的另一路径，
 *   业务需要批量 size + 提交时机控制时用）
 *
 * [AutoConfigureAfter] 在 kudos 体系下有效：`ComponentInitializationDispatcher` 按依赖顺序
 * 调度，本模块在 [ContextAutoConfiguration] 之后初始化。
 *
 * 通过 [StreamConsumerEnvironRegistrar] 启用 multi-binding function.definition 自动聚合。
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

    /** kudos 装配 SPI 用的组件名——务必全模块唯一，与 jar artifact 同名约定。 */
    override fun getComponentName() = "kudos-ability-distributed-stream-rocketmq"

}
