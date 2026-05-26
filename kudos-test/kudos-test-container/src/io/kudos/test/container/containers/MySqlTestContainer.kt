package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * mysql test container.
 *
 * @author K
 * @since 1.0.0
 */
object MySqlTestContainer {

    private const val IMAGE_NAME = "mysql:9.5.0"

    const val PORT = 23306

    const val CONTAINER_PORT = 3306

    const val DATABASE = "test"
    const val USERNAME = "root"
    const val PASSWORD = "mysql"

    const val LABEL = "MySql"

    private val container = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        withEnv("MYSQL_DATABASE", DATABASE)
        withEnv("MYSQL_ROOT_PASSWORD", PASSWORD)
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * Starts the container (if needed).
     *
     * Ensures a single container is shared across a batch of tests, avoiding the time wasted starting/stopping containers repeatedly.
     * Alternatively, you can run this class's main method manually to start the container and share it while running tests.
     * Registers a JVM shutdown hook to automatically stop the container when the batch finishes,
     * rather than stopping after each test — provided the @Testcontainers annotation is not used.
     * To skip tests when Docker is not installed, use @EnabledIfDockerInstalled.
     *
     * @param registry Spring's dynamic property registry, used to register or override already-registered properties
     * @return the running container instance
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(MySqlTestContainer::class.java, "mysql") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Builds the JDBC URL from the running container's actual host/port and registers it as a Spring dynamic property.
     *
     * Uses `DynamicPropertyRegistry.add` instead of setting directly so the value is evaluated lazily
     * during Spring Boot config resolution — avoiding stale reads before the container starts.
     *
     * @param registry the Spring dynamic property registry
     * @param runningContainer the container instance running in Docker
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:mysql://$host:$port/$DATABASE?useSSL=false&useUnicode=true&characterEncoding=utf8"
        registry.add("spring.datasource.dynamic.datasource.mysql.url") { url }
        registry.add("spring.datasource.dynamic.datasource.mysql.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.mysql.password") { PASSWORD }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "MySql")
        startIfNeeded(null)
        println("mysql localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }
}
