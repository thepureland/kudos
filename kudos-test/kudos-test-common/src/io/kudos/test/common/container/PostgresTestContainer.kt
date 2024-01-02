package io.kudos.test.common.container

import org.soul.base.net.IpTool
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer


/**
 * postgres测试容器
 *
 * @author K
 * @since 1.0.0
 */
object PostgresTestContainer {

    private const val IMAGE_NAME = "postgres:15.5"

    const val DATABASE = "test"

    const val PORT = 25432

    private var CONTAINER = FixedHostPortGenericContainer(IMAGE_NAME)
        .withFixedExposedPort(PORT, 5432)
        .withEnv("POSTGRES_DB", DATABASE)
        .withEnv("POSTGRES_USER", "pg")
        .withEnv("POSTGRES_PASSWORD", "postgres")
        .withCommand("postgres -c max_prepared_transactions=10") // seata XA模式需要，不能是默认值0，建议与最大连接数一样


    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val url = "jdbc:postgresql://${IpTool.getLocalIp()}:${PORT}/${DATABASE}"
        registry.add("spring.datasource.dynamic.datasource.postgres.url") { url }
        registry.add("spring.datasource.dynamic.datasource.postgres.username") { CONTAINER.envMap["POSTGRES_USER"] }
        registry.add("spring.datasource.dynamic.datasource.postgres.password") { CONTAINER.envMap["POSTGRES_PASSWORD"] }
    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("postgres localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }
}