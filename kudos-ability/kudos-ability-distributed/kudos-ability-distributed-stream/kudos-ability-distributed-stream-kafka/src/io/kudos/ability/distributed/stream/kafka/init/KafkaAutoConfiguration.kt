package io.kudos.ability.distributed.stream.kafka.init

import jakarta.annotation.PostConstruct
import org.soul.ability.distributed.stream.kafka.starter.StreamKafkaConfiguration
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@ComponentScan(basePackages = ["org.soul.ability.distributed.stream.kafka"])
@PropertySource(
    value = ["classpath:soul-ability-distributed-stream-kafka.yml"],
    factory = SoulPropertySourceFactory::class
)
@ConditionalOnProperty(prefix = "soul.ability.distributed.stream", name = ["enable"], havingValue = "true")
class KafkaAutoConfiguration {
    @PostConstruct
    fun init() {
        LOG.info("[soul-ability-distributed-stream-kafka]初始化完成...")
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(StreamKafkaConfiguration::class.java)
    }
}
