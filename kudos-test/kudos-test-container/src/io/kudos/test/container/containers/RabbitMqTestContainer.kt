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
    
    const val IMAGE_NAME = "rabbitmq:4.1.4-alpine"
    
    const val PORT = 25672

    const val CONTAINER_PORT = 5672

    const val LABEL = "RabbitMQ"

    private val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没安装时想忽略测试用例，可以用@EnabledIfDockerInstalled
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

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
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("rabbit mq localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}

