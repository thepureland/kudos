package io.kudos.test.container

import org.soul.base.net.IpTool
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.FixedHostPortGenericContainer
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

    const val WEB_PORT: Int = 27091

    const val SERVICE_PORT: Int = 28091

    private val testSoulNet: Network? = Network.newNetwork()

    private val CONTAINER: FixedHostPortGenericContainer<*> = FixedHostPortGenericContainer<SELF?>(IMAGE_NAME)
        .withFixedExposedPort(WEB_PORT, 7091)
        .withFixedExposedPort(SERVICE_PORT, 28091)
        .withEnv("SEATA_IP", IpTool.getLocalIp())
        .withNetwork(testSoulNet)
        .withClasspathResourceMapping(
            "seata/seata-server.yml",
            "/seata-server/resources/application.yml",
            BindMode.READ_ONLY
        )
        .waitingFor(Wait.forHttp("/"))

    init {
        // 启动nacos-server
        NacosTestContainer.container.withNetwork(testSoulNet).withNetworkAliases("nacos").start()
    }

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry?) {
    }

    val container: GenericContainer<*>
        get() = CONTAINER

    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        System.out.printf("nacos localhost web-port: %s%n", NacosTestContainer.PORT)
        System.out.printf("seata localhost web-port: %s, service-port：%s%n", WEB_PORT, SERVICE_PORT)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}
