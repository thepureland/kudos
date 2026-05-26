package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.time.Duration


/**
 * MinIO test container.
 *
 * @author K
 * @since 1.0.0
 */
object MinioTestContainer {

    const val IMAGE_NAME = "minio/minio:RELEASE.2025-09-07T16-13-09Z"

    const val PORT = 9000

    const val LABEL = "Minio"

    private val CONTAINER = GenericContainer<Nothing>(IMAGE_NAME).apply {
        withEnv("MINIO_ROOT_USER", "admin")
        withEnv("MINIO_ROOT_PASSWORD", "12345678")
        withExposedPorts(PORT)
        withCommand("server", "/data")
        withCopyFileToContainer(
            MountableFile.forClasspathResource("minio/minio.png"),
            "/docs/0/minio.png"
        )
        // Wait for HTTP 200; the path may be /minio/health/ready or /minio/health/live
        waitingFor(
            Wait.forHttp("/minio/health/ready")
                .forPort(PORT)
                .forStatusCode(200)
        )
        withStartupTimeout(Duration.ofMinutes(1))
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * Start the container if needed.
     *
     * Ensures a single container is shared across batch tests, avoiding the time wasted on repeated
     * starts and stops. The main method of this class can also be run manually to start the container
     * so test cases can share it. A JVM shutdown hook is registered so the container is stopped
     * automatically when the batch test ends rather than after each test case, provided the
     * @Testcontainers annotation is not used. Use @EnabledIfDockerInstalled to skip test cases when
     * Docker is not installed.
     *
     * @param registry Spring's dynamic property registry, used to register or override registered properties
     * @return the running container
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(MinioTestContainer::class.java, "minio") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Registers the MinIO endpoint into Spring dynamic properties. Internal and external endpoints
     * both point to the same URL — under test, the Docker host is reachable and there is no need to
     * distinguish public vs. internal networks.
     *
     * @param registry Spring's dynamic property registry
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val endPoint = "http://$host:$port"
        registry.add("kudos.ability.file.minio.endpoint") { endPoint }
        registry.add("kudos.ability.file.minio.public-endpoint") { endPoint }
    }

    /**
     * Returns the running container.
     *
     * @return the container, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Minio")
        startIfNeeded(null)
        println("minio localhost port: " + CONTAINER.firstMappedPort)
        Thread.sleep(Long.MAX_VALUE)
    }

}