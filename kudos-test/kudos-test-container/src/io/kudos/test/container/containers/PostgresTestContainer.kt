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

    private const val IMAGE_NAME = "postgres:18.0-alpine3.22"

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


    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没启动时想忽略测试用例，可以用@EnabledIfDockerAvailable
     * 来代替@Testcontainers(disabledWithoutDocker = true)
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer: Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:postgresql://$host:$port/$DATABASE"
        registry.add("spring.datasource.dynamic.datasource.postgres.url") { url }
        registry.add("spring.datasource.dynamic.datasource.postgres.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.postgres.password") { PASSWORD }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("postgres localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
