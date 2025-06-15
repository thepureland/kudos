package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * mysql测试容器
 *
 * @author K
 * @since 1.0.0
 */
object MySqlTestContainer {

    private const val IMAGE_NAME = "mysql:5.7.44"

    const val PORT = 23306

    const val CONTAINER_PORT = 3306

    const val DATABASE = "test"
    const val USERNAME = "root"
    const val PASSWORD = "mysql"

    const val LABEL = "MySql"

    val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        withEnv("MYSQL_DATABASE", DATABASE)
        withEnv("MYSQL_ROOT_PASSWORD", PASSWORD)
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

        val url = "jdbc:mysql://$host:$port/$DATABASE?useSSL=false&useUnicode=true&characterEncoding=utf8"
        registry.add("spring.datasource.dynamic.datasource.mysql.url") { url }
        registry.add("spring.datasource.dynamic.datasource.mysql.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.mysql.password") { PASSWORD }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("mysql localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}
