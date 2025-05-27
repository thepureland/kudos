package io.kudos.test.container

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
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

    val container = GenericContainer<Nothing>(IMAGE_NAME).apply {
        withEnv("MODE", "standalone")

        // 1. 声明容器要“暴露”的端口（必须和镜像 Dockerfile EXPOSE 一致）
        exposedPorts = listOf(8848, 9848, 9849)

        // 2. 再绑定宿主机端口 -> 容器端口
        withCreateContainerCmdModifier { cmd ->
            cmd.hostConfig!!.withPortBindings(
                PortBinding(Ports.Binding.bindPort(PORT), ExposedPort(8848)),
                PortBinding(Ports.Binding.bindPort(29848), ExposedPort(9848)),
                PortBinding(Ports.Binding.bindPort(29849), ExposedPort(9849))
            )
        }

        // 3. 等待容器内的 8848 端口就绪
        waitingFor(Wait.forHttp("/nacos").forPort(8848))
    }


    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("nacos localhost port: " + PORT)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
