package io.kudos.test.container

import org.soul.base.net.IpTool
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer
import java.util.function.Supplier

/**
 * rabbit mq测试容器
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
object RabbitMqTestContainer {
    const val IMAGE_NAME: String = "rabbitmq:3.7.25-management-alpine"
    const val PORT: Int = 25672
    val container: FixedHostPortGenericContainer<*> = FixedHostPortGenericContainer<Any?>(IMAGE_NAME)
        .withFixedExposedPort(PORT, 5672)

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("soul.ability.distributed.stream.mq-config.rabbitmq.host", Supplier { IpTool.getLocalIp() })
        registry.add("soul.ability.distributed.stream.mq-config.rabbitmq.username", Supplier { "guest" })
        registry.add("soul.ability.distributed.stream.mq-config.rabbitmq.password", Supplier { "guest" })
        registry.add("soul.ability.distributed.stream.mq-config.rabbitmq.port", Supplier { PORT })
        registry.add("soul.ability.distributed.stream.mq-config.rabbitmq.virtual-host", Supplier { "/" })
        registry.add(
            "soul.ability.distributed.stream.mq-config.rabbitmq.addresses",
            Supplier { IpTool.getLocalIp() + ":" + PORT })
    }
}
