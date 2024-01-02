package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * nacos-server测试容器
 *
 * @author K
 * @since 1.0.0
 */
object NacosTestContainer {

    private const val IMAGE_NAME = "nacos/nacos-server:v2.2.3"

    const val PORT = 28848

    private var CONTAINER = FixedHostPortGenericContainer(IMAGE_NAME)
        .withEnv("MODE", "standalone")
        .withFixedExposedPort(PORT, 8848)
        .withFixedExposedPort(29848, 9848)
        .withFixedExposedPort(29849, 9849)
        .waitingFor(Wait.forHttp("/nacos"))


    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {

    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("nacos localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}