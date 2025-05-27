package io.kudos.ability.distributed.stream.rabbit.producer

import io.kudos.context.init.EnableKudos
import io.kudos.context.spring.YamlPropertySourceFactory
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.kafka.annotation.EnableKafka

/**
 * kafka测试生產者微服务应用
 *
 * @author shane
 * @author K
 * @since 5.1.1
 */
@EnableKudos
@EnableDiscoveryClient
@PropertySource(
    value = ["classpath:application-kafka-producer.yml"
    ], factory = YamlPropertySourceFactory::class
)
@Import(KafkaProducerController::class, KafkaProducerService::class)
open class KafkaProducerApplication
