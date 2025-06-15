package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

/**
 * kafka测试容器
 *
 * @author K
 * @since 1.0.0
 */
object KafkaTestContainer {

    private const val IMAGE_NAME = "confluentinc/cp-kafka:7.9.1"

    private var imageName = DockerImageName
        .parse(IMAGE_NAME)
        .asCompatibleSubstituteFor("apache/kafka")

    const val LABEL = "Kafka"

    val container = ConfluentKafkaContainer(imageName).withLabel(TestContainerKit.LABEL_KEY, LABEL)


    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container, this)
        if (registry != null) {
            registerProperties(registry, runningContainer)
        }
        return runningContainer
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        registry.add("spring.cloud.stream.kafka.binder.brokers") { "$host:$port" }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("kafka started.")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
