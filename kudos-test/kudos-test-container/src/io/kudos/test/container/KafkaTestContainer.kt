package io.kudos.test.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.function.Supplier

object KafkaTestContainer {
    private const val IMAGE_NAME = "confluentinc/cp-kafka:7.4.0"

    val container: KafkaContainer = KafkaContainer(DockerImageName.parse(IMAGE_NAME))

    fun start(registry: DynamicPropertyRegistry?): KafkaContainer {
        container.start() // 防止属性注册时，容器还未启动完成.
        if (registry != null) {
            registerProperties(registry)
        }
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add(
            "kudos.ability.distributed.stream.mq-config.kafka-brokers",
            Supplier { container.getHost() + ":" + container.getFirstMappedPort() })
    }
}
