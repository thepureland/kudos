package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName


/**
 * InfluxDb测试容器
 *
 * @author K
 * @since 1.0.0
 */
object InfluxTestContainer {

    private const val IMAGE_NAME = "influxdb:2.7.4-alpine"

    private var CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME))
        .withExposedPorts(8086)
        .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
        .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
        .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "admin123")
        .withEnv("DOCKER_INFLUXDB_INIT_ORG", "soul-org")
        .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", "soul-bucket")
        .withEnv(
            "DOCKER_INFLUXDB_INIT_ADMIN_TOKEN",
            "QNlXS0zPLoBDNCMA5zxWRinigwEUClf2BfM9Ivp4zMuuHeCN_bEHOEPxA50asUhHXbtDHlkpXDIycGEMOlJFfw=="
        )

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val url = "http://localhost:${CONTAINER.firstMappedPort}"

        // 单客户端配置
        registry.add("soul.ability.influxdb.url") { url }

        // 数据源 server1
        registry.add("soul.ability.influxdb.multi.server1.url") { url }

        // 数据源 server2
        registry.add("soul.ability.influxdb.multi.server2.url") { url }

        // 数据源 server3
        registry.add("soul.ability.influxdb.multi.server3.url") { url }
    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("InfluxDb localhost port: ${CONTAINER.getFirstMappedPort()}")
        Thread.sleep(Long.MAX_VALUE)
    }

}