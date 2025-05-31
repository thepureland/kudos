package io.kudos.test.container

import org.soul.base.net.IpTool
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.FixedHostPortGenericContainer
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

    private val CONTAINER = FixedHostPortGenericContainer(IMAGE_NAME)
        .withFixedExposedPort(WEB_PORT, 7091)
        .withFixedExposedPort(SERVICE_PORT, 28091)
        .withEnv("SEATA_IP", IpTool.getLocalIp())
        .withNetwork(testSoulNet)
        .withClasspathResourceMapping(
            "seata/seata-server.yml",
            "/seata-server/resources/application.yml",
            BindMode.READ_ONLY
        )
//        .waitingFor(Wait.forHttp("/").forPort(7091))
        .waitingFor(Wait.forListeningPort())

    init {
        // 启动nacos-server
        NacosTestContainer.container.withNetwork(testSoulNet).withNetworkAliases("nacos").start()
    }

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        println(">>>>>>>>>>>>>>>>>>>> Starting Seata container...")
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> Seata container started.")
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry?) {
    }

    val container: FixedHostPortGenericContainer<*>? = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("nacos localhost web-port: ${NacosTestContainer.PORT}")
        println("seata localhost web-port: $WEB_PORT, service-port：$SERVICE_PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
