package io.kudos.test.container

import io.kudos.base.net.IpKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer

/**
 * rabbit mq测试容器
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
object RabbitMqTestContainer {
    
    const val IMAGE_NAME = "rabbitmq:3.7.25-management-alpine"
    
    const val PORT = 25672
    
    val container = FixedHostPortGenericContainer(IMAGE_NAME)
        .withFixedExposedPort(PORT, 5672)

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        println(">>>>>>>>>>>>>>>>>>>> Starting RabbitMQ container...")
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> RabbitMQ container started.")
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.rabbitmq.host") { IpKit.getLocalIp() }
        registry.add("spring.rabbitmq.username") { "guest" }
        registry.add("spring.rabbitmq.password") { "guest" }
        registry.add("spring.rabbitmq.port") { PORT }
        registry.add("spring.rabbitmq.virtual-host") { "/" }
        registry.add("spring.rabbitmq.addresses") { "${IpKit.getLocalIp()}:$PORT" }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("rabbit mq localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
