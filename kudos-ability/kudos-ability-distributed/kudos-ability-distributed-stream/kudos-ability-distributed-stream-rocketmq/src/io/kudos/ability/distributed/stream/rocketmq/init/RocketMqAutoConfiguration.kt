package io.kudos.ability.distributed.stream.rocketmq.init

import io.kudos.ability.distributed.stream.common.init.StreamCommonConfiguration
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * RocketMq自动配置类
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
//@Import(StreamConsumerEnvironRegistrar::class)
open class RocketMqAutoConfiguration : StreamCommonConfiguration(), IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-stream-rocketmq"

}