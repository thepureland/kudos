package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

/**
 * Kafka test container.
 *
 * @author K
 * @since 1.0.0
 */
object KafkaTestContainer {

    private const val IMAGE_NAME = "apache/kafka-native:4.1.1"

    private val imageName = DockerImageName
        .parse(IMAGE_NAME)
//        .asCompatibleSubstituteFor("apache/kafka")

    const val LABEL = "Kafka"

    private val container = KafkaContainer(imageName).withLabel(TestContainerKit.LABEL_KEY, LABEL)


    /**
     * Start the container if needed.
     *
     * Ensures a single container is shared across batch tests, avoiding the time wasted on repeated
     * starts and stops. The main method of this class can also be run manually to start the container
     * so test cases can share it. A JVM shutdown hook is registered so the container is stopped
     * automatically when the batch test ends rather than after each test case, provided the
     * @Testcontainers annotation is not used. Use @EnabledIfDockerInstalled to skip test cases when
     * Docker is not installed.
     *
     * @param registry Spring's dynamic property registry, used to register or override registered properties
     * @return the running container
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(KafkaTestContainer::class.java, "kafka") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Combines the container's actual host:port into a Kafka broker address and registers it into
     * the Spring Cloud Stream Kafka binder configuration.
     *
     * @param registry Spring's dynamic property registry
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        registry.add("spring.cloud.stream.kafka.binder.brokers") { "$host:$port" }
    }

    /**
     * Returns the running container.
     *
     * @return the container, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Kafka")
        startIfNeeded(null)
        println("kafka started.")
        Thread.sleep(Long.MAX_VALUE)
    }

}
