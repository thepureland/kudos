package io.kudos.test.container

import io.kudos.base.logger.LogFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

/**
 * kafka测试容器
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
object KafkaTestContainer {

    private const val IMAGE_NAME = "confluentinc/cp-kafka:7.9.1"

    private var imageName = DockerImageName
        .parse(IMAGE_NAME)
        .asCompatibleSubstituteFor("apache/kafka")

    val container = ConfluentKafkaContainer(imageName)


    fun start(registry: DynamicPropertyRegistry?): ConfluentKafkaContainer {
        LogFactory.getLog(this).info("Starting Kafka container...")
        println(">>>>>>>>>>>>>>>>>>>> Starting Kafka container...")
        container.start() // 防止属性注册时，容器还未启动完成.
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> Kafka container started.")
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers") {
            "${container.host}:${container.firstMappedPort}"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("kafka started.")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
