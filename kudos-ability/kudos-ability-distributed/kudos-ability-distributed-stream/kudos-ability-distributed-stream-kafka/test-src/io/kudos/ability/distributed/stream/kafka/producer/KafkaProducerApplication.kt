package io.kudos.ability.distributed.stream.kafka.producer

import org.soul.context.context.EnableSoul
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * kafka 生產者微服务应用
 *
 * @author shane
 * @since 5.1.1
 */
@EnableDiscoveryClient
@PropertySource(
    value = ["classpath:application-kafka-producer.yml"
    ], factory = SoulPropertySourceFactory::class
)
@EnableSoul
@SpringBootApplication
@Import(KafkaProducerController::class, KafkaProducerService::class)
class KafkaProducerApplication 
