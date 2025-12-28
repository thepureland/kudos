package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.sql.DriverManager


/**
 * postgres向量數據庫測試容器
 *
 * @author K
 * @since 1.0.0
 */
object PgVectorTestContainer {

    private const val IMAGE_NAME = "pgvector/pgvector:0.8.1-pg18-trixie"

    const val DATABASE = "test"

    const val PORT = 25433

    const val CONTAINER_PORT = 5432

    const val USERNAME = "pg"

    const val PASSWORD = "postgres"

    const val LABEL = "Pg-vector"


    val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        withEnv("POSTGRES_DB", DATABASE)
        withEnv("POSTGRES_USER", USERNAME)
        withEnv("POSTGRES_PASSWORD", PASSWORD)
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
        waitingFor(Wait.forListeningPort())
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
            val running = TestContainerKit.isContainerRunning(LABEL)
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (!running) { // first time start, init after start.
                // 启用扩展
                val result = container.execInContainer(
                    "bash", "-lc",
                    """
                    set -euo pipefail
                
                    USER="${'$'}{POSTGRES_USER:-postgres}"
                    DB="${'$'}{POSTGRES_DB:-${'$'}USER}"
                    PASS="${'$'}{POSTGRES_PASSWORD:-}"
                
                    export PGPASSWORD="${'$'}PASS"
                
                    psql -h 127.0.0.1 -p 5432 -U "${'$'}USER" -d "${'$'}DB" -v ON_ERROR_STOP=1 -c "
                      CREATE EXTENSION IF NOT EXISTS vector;
                      CREATE EXTENSION IF NOT EXISTS hstore;
                      CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";
                    "
                    """.trimIndent()
                )

                require(result.exitCode == 0) {
                    "Init extensions failed.\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}"
                }

            }
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
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("postgres(include pgvector) localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}