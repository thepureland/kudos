package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * rabbit mq测试容器
 *
 * @author K
 * @since 1.0.0
 */
object RabbitMqTestContainer {
    
    const val IMAGE_NAME = "rabbitmq:3.7.25-management-alpine"
    
    const val PORT = 25672

    const val CONTAINER_PORT = 5672

    const val LABEL = "RabbitMQ"

    val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


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

        registry.add("spring.rabbitmq.host") { host }
        registry.add("spring.rabbitmq.username") { "guest" }
        registry.add("spring.rabbitmq.password") { "guest" }
        registry.add("spring.rabbitmq.port") { port }
        registry.add("spring.rabbitmq.virtual-host") { "/" }
        registry.add("spring.rabbitmq.addresses") { "$host:$port" }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("rabbit mq localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}

