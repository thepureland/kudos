package io.kudos.test.container

import org.soul.base.net.IpTool
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer
import java.util.function.Supplier

/**
 * postgres测试容器
 *
 * @author will
 * @since 5.1.1
 */
object PostgresTestContainer {
    private const val IMAGE_NAME = "postgres:14.10"

    const val DATABASE: String = "test"

    const val PORT: Int = 25432

    val container: FixedHostPortGenericContainer<*> = FixedHostPortGenericContainer<SELF?>(IMAGE_NAME)
        .withFixedExposedPort(PORT, 5432)
        .withEnv("POSTGRES_DB", DATABASE)
        .withEnv("POSTGRES_USER", "pg")
        .withEnv("POSTGRES_PASSWORD", "postgres")
        .withCommand("postgres -c max_prepared_transactions=10")
    // seata XA模式需要，不能是默认值0，建议与最大连接数一样

    fun start(registry: DynamicPropertyRegistry?): FixedHostPortGenericContainer<*> {
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val url: String = "jdbc:postgresql://%s:%s/%s".formatted(IpTool.getLocalIp(), PORT, DATABASE)
        registry.add("spring.datasource.dynamic.datasource.postgres.url", Supplier { url })
    }

    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("postgres localhost port: " + PORT)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}
