package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName


/**
 * MariaDB测试容器
 *
 * @author K
 * @since 1.0.0
 */
object MariaTestContainer {

    private const val IMAGE_NAME = "mariadb:11.2.2"

    private const val DATABASE = "test"

    private var CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME))
        .withEnv("MYSQL_DATABASE", "test")
        .withEnv("MARIADB_ROOT_PASSWORD", "maria")
        .withExposedPorts(8080)

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val url = "jdbc:mariadb://localhost:${CONTAINER.firstMappedPort}/${DATABASE}"
        registry.add("spring.datasource.dynamic.datasource.maria.url") { url }
        registry.add("spring.datasource.dynamic.datasource.maria.username") { "root" }
        registry.add("spring.datasource.dynamic.datasource.maria.password") { CONTAINER.envMap["MARIADB_ROOT_PASSWORD"] }
    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("MariaDB localhost port: ${CONTAINER.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}