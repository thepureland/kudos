package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName


/**
 * WireMock test container.
 *
 * @author K
 * @since 1.0.0
 */
object WireMockTestContainer {

    const val LABEL = "WireMock"

    private val container = GenericContainer(DockerImageName.parse("wiremock/wiremock:3.13.2-1-alpine"))
        .withExposedPorts(8080)
        .withCommand("--global-response-templating")
        .waitingFor(Wait.forHttp("/__admin").forStatusCode(200))
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)


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
        return TestContainerCrossProcessLock.run(WireMockTestContainer::class.java, "wiremock") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * The WireMock container currently registers no Spring properties — test code uses
     * [getRunningContainer] directly to build its baseUrl, with no need to pass values into bean configuration.
     * The method is kept to keep the [startIfNeeded] call template aligned with other TestContainer types.
     *
     * @param registry the Spring dynamic property registry (null allowed)
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "WireMock")
        startIfNeeded(null)
        println("WireMock localhost port: ${container.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}