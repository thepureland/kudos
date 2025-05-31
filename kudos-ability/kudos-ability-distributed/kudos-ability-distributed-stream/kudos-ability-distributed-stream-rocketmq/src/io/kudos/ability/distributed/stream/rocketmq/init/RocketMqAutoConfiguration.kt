package io.kudos.ability.distributed.stream.rocketmq.init

import io.kudos.ability.distributed.stream.common.init.StreamCommonConfiguration
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * RocketMq自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-distributed-stream-common.yml", "classpath:kudos-ability-distributed-stream-rocketmq.yml"],
    factory = YamlPropertySourceFactory::class
)
//@Import(StreamConsumerEnvironRegistrar::class)
open class RocketMqAutoConfiguration : StreamCommonConfiguration(), IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-stream-rocketmq"

}