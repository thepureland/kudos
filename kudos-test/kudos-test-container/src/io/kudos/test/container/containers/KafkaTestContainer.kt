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

    private val container = ConfluentKafkaContainer(imageName).withLabel(TestContainerKit.LABEL_KEY, LABEL)


    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没启动时想忽略测试用例，可以用@EnabledIfDockerAvailable
     * 来代替@Testcontainers(disabledWithoutDocker = true)
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
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        registry.add("spring.cloud.stream.kafka.binder.brokers") { "$host:$port" }
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
        println("kafka started.")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
