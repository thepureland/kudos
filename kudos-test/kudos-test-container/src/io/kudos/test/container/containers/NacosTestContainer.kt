package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * nacos-server测试容器
 *
 * @author K
 * @since 1.0.0
 */
object NacosTestContainer {

    private const val IMAGE_NAME = "nacos/nacos-server:v2.2.3-slim"

    const val PORT = 28848

    const val LABEL = "Nacos"

    val container = GenericContainer(IMAGE_NAME).apply {
        withEnv("MODE", "standalone")

        // 1. 声明容器要“暴露”的端口（必须和镜像 Dockerfile EXPOSE 一致）
        exposedPorts = listOf(8848, 9848, 9849)

        // 2. 再绑定宿主机端口 -> 容器端口
        bindingPort(Pair(PORT, 8848), Pair(29848, 9848), Pair(29849, 9849))

        // 3. 等待容器内的 8848 端口就绪
        waitingFor(Wait.forHttp("/nacos").forPort(8848))

        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container, this)
        if (registry != null) {
            registerProperties(registry, runningContainer)
        }
        return runningContainer
    }

    internal fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort
        val serverAddr = "$host:$port"

        registry.add("spring.cloud.nacos.config.server-addr") { serverAddr }
        registry.add("spring.cloud.nacos.discovery.server-addr") { serverAddr }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("nacos localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
