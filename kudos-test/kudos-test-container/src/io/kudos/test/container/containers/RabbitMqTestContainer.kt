package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * RabbitMQ test container.
 *
 * @author K
 * @since 1.0.0
 */
object RabbitMqTestContainer {
    
    const val IMAGE_NAME = "rabbitmq:4.1.4-alpine"
    
    const val PORT = 25672

    const val CONTAINER_PORT = 5672

    const val LABEL = "RabbitMQ"

    private val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(PORT to CONTAINER_PORT)
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


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
        return TestContainerCrossProcessLock.run(RabbitMqTestContainer::class.java, "rabbitmq") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Registers the running container's host/port into Spring AMQP properties (default guest/guest
     * credentials + root vhost). `requireNotNull` guards against the Docker API occasionally returning
     * empty fields and silently producing misaligned configuration — fail fast for easier diagnosis.
     *
     * @param registry Spring's dynamic property registry
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val firstPort = runningContainer.ports.first()
        val host = requireNotNull(firstPort.ip) { "container port ip is null" }
        val port = requireNotNull(firstPort.publicPort) { "container publicPort is null" }

        registry.add("spring.rabbitmq.host") { host }
        registry.add("spring.rabbitmq.username") { "guest" }
        registry.add("spring.rabbitmq.password") { "guest" }
        registry.add("spring.rabbitmq.port") { port }
        registry.add("spring.rabbitmq.virtual-host") { "/" }
        registry.add("spring.rabbitmq.addresses") { "$host:$port" }
    }

    /**
     * Returns the running container.
     *
     * @return the container, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "RabbitMQ")
        startIfNeeded(null)
        println("rabbit mq localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}

