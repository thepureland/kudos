package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * h2测试容器
 *
 * @author K
 * @since 1.0.0
 */
object H2TestContainer {

    private const val IMAGE_NAME = "oscarfonts/h2:2.1.210"

    const val DATABASE = "test"

    const val PORT = 1521

    const val USERNAME = "sa"

    const val PASSWORD = ""

    const val LABEL = "H2"

    val container = GenericContainer(IMAGE_NAME).apply {
        // 通过环境变量指定 H2 启动参数：TCP 模式，允许外部访问，不存在时创建数据库
        withEnv("H2_OPTIONS", "-tcp -tcpAllowOthers -ifNotExists")
        withExposedPorts(PORT)
        bindingPort(Pair(PORT, PORT))
        waitingFor(Wait.forListeningPort())
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
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

        val url = "jdbc:h2:tcp://$host:$port/mem:$DATABASE;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
        registry.add("spring.datasource.dynamic.datasource.h2.url") { url }
        registry.add("spring.datasource.dynamic.datasource.h2.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.h2.password") { PASSWORD }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("H2 ${container.host} port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
