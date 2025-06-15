package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * postgres测试容器
 *
 * @author K
 * @since 1.0.0
 */
object PostgresTestContainer {

    private const val IMAGE_NAME = "postgres:14.10"

    const val DATABASE = "test"

    const val PORT = 25432

    const val CONTAINER_PORT = 5432

    const val USERNAME = "pg"

    const val PASSWORD = "postgres"

    const val LABEL = "PostgreSql"

    val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        withEnv("POSTGRES_DB", DATABASE)
        withEnv("POSTGRES_USER", USERNAME)
        withEnv("POSTGRES_PASSWORD", PASSWORD)
        withCommand("postgres -c max_prepared_transactions=10") // seata XA模式需要，不能是默认值0，建议与最大连接数一样
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container, this)
        if (registry != null) {
            registerProperties(registry, runningContainer)
        }
        return runningContainer
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer: Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:postgresql://$host:$port/$DATABASE"
        registry.add("spring.datasource.dynamic.datasource.postgres.url") { url }
        registry.add("spring.datasource.dynamic.datasource.postgres.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.postgres.password") { PASSWORD }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("postgres localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
