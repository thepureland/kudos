package io.kudos.test.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer

/**
 * mysql测试容器
 *
 * @author will
 * @since 5.1.1
 */
object MySqlTestContainer {

    private const val IMAGE_NAME = "mysql:5.7.44"

    const val PORT = 23306
    const val DATABASE = "test"
    const val USERNAME = "root"
    const val PASSWORD = "mysql"

    val container = FixedHostPortGenericContainer(IMAGE_NAME)
        .withFixedExposedPort(PORT, 3306)
        .withEnv("MYSQL_DATABASE", DATABASE)
        .withEnv("MYSQL_ROOT_PASSWORD", PASSWORD)

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        println(">>>>>>>>>>>>>>>>>>>> Starting MySql container...")
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> MySql container started.")
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val url = "jdbc:mysql://localhost:$PORT/$DATABASE?useSSL=false&useUnicode=true&characterEncoding=utf8"
        registry.add("spring.datasource.dynamic.datasource.mysql.url") { url }
        registry.add("spring.datasource.dynamic.datasource.mysql.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.mysql.password") { PASSWORD }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("mysql localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}
