package io.kudos.test.container

import io.kudos.base.net.IpKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer

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

    const val USERNAME = "pg"

    const val PASSWORD = "postgres"

    val container = FixedHostPortGenericContainer(IMAGE_NAME)
        .withFixedExposedPort(PORT, 5432)
        .withEnv("POSTGRES_DB", DATABASE)
        .withEnv("POSTGRES_USER", USERNAME)
        .withEnv("POSTGRES_PASSWORD", PASSWORD)
        .withCommand("postgres -c max_prepared_transactions=10") // seata XA模式需要，不能是默认值0，建议与最大连接数一样


    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        println(">>>>>>>>>>>>>>>>>>>> Starting postgres container...")
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> Postgres container started.")
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val url = "jdbc:postgresql://${IpKit.getLocalIp()}:$PORT/$DATABASE"
        registry.add("spring.datasource.dynamic.datasource.postgres.url") { url }
        registry.add("spring.datasource.dynamic.datasource.postgres.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.postgres.password") { PASSWORD }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("postgres localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
