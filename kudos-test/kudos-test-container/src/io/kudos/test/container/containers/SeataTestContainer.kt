package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.net.IpKit
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait

/**
 * seata-server测试容器
 *
 * @author K
 * @since 1.0.0
 */
object SeataTestContainer {

    private const val IMAGE_NAME = "seataio/seata-server:2.0.0-slim"

    const val WEB_PORT = 27091

    const val SERVICE_PORT = 28091

    private val testSoulNet = Network.newNetwork()

    const val LABEL = "Seata"

    private lateinit var runningNacosContainer : Container

    val CONTAINER = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(7091, 28091)
        bindingPort(Pair(WEB_PORT, 7091), Pair(SERVICE_PORT, 28091))
        withEnv("SEATA_IP", IpKit.getLocalIp())
        withNetwork(testSoulNet)
        withClasspathResourceMapping(
            "seata/seata-server.yml",
            "/seata-server/resources/application.yml",
            BindMode.READ_ONLY
        )
//        waitingFor(Wait.forHttp("/").forPort(7091))
        waitingFor(Wait.forListeningPort())
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        startNacos(registry)

        val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER, this)
        if (registry != null) {
            registerProperties(registry, runningContainer)
        }
        return runningContainer
    }

    // 启动nacos-server
    private fun startNacos(registry: DynamicPropertyRegistry?) {
        val nacosLabel = "nacos-server"
        val nacos = NacosTestContainer.container.apply {
            withNetwork(testSoulNet)
            withNetworkAliases("nacos")
            bindingPort(Pair(38848, 8848), Pair(39848, 9848), Pair(39849, 9849))
            withLabel(TestContainerKit.LABEL_KEY, nacosLabel)
        }
        runningNacosContainer = TestContainerKit.startContainerIfNeeded(nacosLabel, nacos, this)
        if (registry != null) {
            NacosTestContainer.registerProperties(registry, runningNacosContainer)
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("nacos localhost web-port: ${runningNacosContainer.ports.first().publicPort}")
        println("seata localhost web-port: $WEB_PORT, service-port：$SERVICE_PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
