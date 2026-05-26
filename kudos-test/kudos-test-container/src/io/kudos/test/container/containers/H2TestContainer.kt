package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * h2 test container.
 *
 * @author K
 * @since 1.0.0
 */
object H2TestContainer {

    /**
     * H2 test image.
     *
     * The default `oscarfonts/h2:alpine` only publishes an amd64 manifest. On arm64 hosts such as Apple Silicon,
     * this triggers testcontainers' architecture-mismatch warning and falls back to Rosetta/QEMU emulation, which is slow to start and occasionally times out.
     * Override with `-Dkudos.test.h2.image=<image>` or the `KUDOS_TEST_H2_IMAGE` environment variable
     * to use a local build or a third-party multi-arch image (e.g. a custom `eclipse-temurin` + h2.jar image).
     */
    private val IMAGE_NAME: String =
        System.getProperty("kudos.test.h2.image")
            ?: System.getenv("KUDOS_TEST_H2_IMAGE")
            ?: "oscarfonts/h2:alpine"

    const val DATABASE = "test"

    const val PORT = 1521

    const val USERNAME = "sa"

    const val PASSWORD = "sa"

    const val LABEL = "H2"

    private val container = GenericContainer(IMAGE_NAME).apply {
        // Specify H2 startup args via env var: TCP mode, allow external access, create database if it does not exist
        withEnv("H2_OPTIONS", "-tcp -tcpAllowOthers -ifNotExists")
        withExposedPorts(PORT)
        bindingPort(Pair(PORT, PORT))
        waitingFor(Wait.forListeningPort())
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
        return TestContainerCrossProcessLock.run(H2TestContainer::class.java, "h2") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Builds the H2 remote in-memory JDBC URL (`DB_CLOSE_DELAY=-1` prevents the in-memory DB from being released when the last connection closes)
     * and registers it as a Spring dynamic property. Uses `add` so the value is evaluated lazily during config resolution, avoiding stale reads before the container starts.
     *
     * @param registry the Spring dynamic property registry
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:h2:tcp://$host:$port/mem:$DATABASE;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
        registry.add("spring.datasource.dynamic.datasource.h2.url") { url }
        registry.add("spring.datasource.dynamic.datasource.h2.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.h2.password") { PASSWORD }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "H2")
        startIfNeeded(null)
        println("H2 ${container.host} port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}
