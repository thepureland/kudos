package io.kudos.ability.distributed.stream.rabbit.producer

import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.EnableKudos
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * RabbitMq测试生產者微服务应用
 *
 * @author shane
 * @author K
 * @since 5.1.1
 */
@EnableKudos
@EnableDiscoveryClient
@PropertySource(
    value = ["classpath:application-rabbit-producer.yml"
    ], factory = YamlPropertySourceFactory::class
)
@Import(RabbitMqProducerController::class, RabbitMqProducerService::class)
open class RabbitMqProducerApplication
