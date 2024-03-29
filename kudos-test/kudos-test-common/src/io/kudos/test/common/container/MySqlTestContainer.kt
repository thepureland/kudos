package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer


/**
 * mysql测试容器
 *
 * @author K
 * @since 1.0.0
 */
object MySqlTestContainer {

    private const val IMAGE_NAME = "mysql:5.7.44"

    const val PORT = 23306

    const val DATABASE = "test"

    private var CONTAINER = FixedHostPortGenericContainer(IMAGE_NAME)
        .withFixedExposedPort(PORT, 3306)
        .withEnv("MYSQL_DATABASE", DATABASE)
        .withEnv("MYSQL_ROOT_PASSWORD", "mysql")

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.dynamic.datasource.mysql.url") {
            "jdbc:mysql://${CONTAINER.host}:${PORT}/${CONTAINER.envMap["MYSQL_DATABASE"]}"
        }
        registry.add("spring.datasource.dynamic.datasource.mysql.username") { "root" }
        registry.add("spring.datasource.dynamic.datasource.mysql.password") { CONTAINER.envMap["MYSQL_ROOT_PASSWORD"] }
    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("mysql localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}