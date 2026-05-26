package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import java.security.SecureRandom
import java.util.Base64

/**
 * nacos-server test container.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object NacosTestContainer {

    private const val IMAGE_NAME = "nacos/nacos-server:v3.1.1-slim"

    const val PORT = 28848

    const val LABEL = "Nacos"

    const val LABEL_NACOS_FOR_SEATA = "Nacos (for Seata)"

    val tokenBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }

    val tokenBase64: String = Base64.getEncoder().encodeToString(tokenBytes)

    private val container = GenericContainer(IMAGE_NAME).apply {
        withEnv("MODE", "standalone")
        withEnv("PREFER_HOST_MODE", "ip")
        withEnv("NACOS_AUTH_ENABLE", "false")             // Disable auth (3.x essentially requires it by default)
        withEnv("NACOS_AUTH_TOKEN", tokenBase64)  // Required: Base64 string
        withEnv("NACOS_AUTH_IDENTITY_KEY", "nacos")
        withEnv("NACOS_AUTH_IDENTITY_VALUE", "nacos")

        // 1. Declare ports the container will "expose" (must match the image's Dockerfile EXPOSE)
        exposedPorts = listOf(8848, 9848, 9849)

        // 2. Bind host ports -> container ports
        bindingPort(Pair(PORT, 8848), Pair(29848, 9848), Pair(29849, 9849))

        // 3. Nacos 8848 returns a page early, but naming/config gRPC may not be ready yet; probe the API and relax the startup timeout.
        waitingFor(
            Wait.forHttp("/nacos/v1/ns/instance/list?serviceName=__readiness")
                .forPort(8848)
                .withStartupTimeout(Duration.ofSeconds(90))
        )

        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    val containerForSeata = GenericContainer(IMAGE_NAME).apply {
        withEnv("MODE", "standalone")
        withEnv("NACOS_AUTH_ENABLE", "false")             // Disable auth (3.x essentially requires it by default)
        withEnv("NACOS_AUTH_TOKEN", tokenBase64)  // Required: Base64 string
        withEnv("NACOS_AUTH_IDENTITY_KEY", "nacos")
        withEnv("NACOS_AUTH_IDENTITY_VALUE", "nacos")
        withNetwork(TestContainerKit.DEFAULT_DOCKER_NETWORK)
        withNetworkAliases("nacos")
        exposedPorts = listOf(8848, 9848, 9849)
        bindingPort(Pair(38848, 8848), Pair(39848, 9848), Pair(39849, 9849))
        // Nacos 8848 returns 200 for `/nacos` early on, but the internal naming registry still needs a few more seconds to accept registration writes.
        // Probe the v1 instance-query API directly here: only treat it as truly available after a 200 response, to avoid the Seata server starting up and being
        // rejected with "server is DOWNnow, please try again later", which would fail the overall startup.
        waitingFor(
            Wait.forHttp("/nacos/v1/ns/instance/list?serviceName=__readiness")
                .forPort(8848)
                .withStartupTimeout(Duration.ofSeconds(90))
        )
        withLabel(TestContainerKit.LABEL_KEY, LABEL_NACOS_FOR_SEATA)
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
        return TestContainerCrossProcessLock.run(NacosTestContainer::class.java, "nacos") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    fun startNacosForSeataIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(NacosTestContainer::class.java, "nacos-seata") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL_NACOS_FOR_SEATA, containerForSeata)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    internal fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
//        val host = runningContainer.ports.first().ip
        val host = "localhost"
//        val port = runningContainer.ports.first().publicPort
        val port = "38848"
        val serverAddr = "$host:$port"

        registry.add("spring.cloud.nacos.config.server-addr") { serverAddr }
        registry.add("spring.cloud.nacos.discovery.server-addr") { serverAddr }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Nacos")
        ManualTestContainerMainSupport.removeExistingContainers(LABEL_NACOS_FOR_SEATA, "Nacos (for Seata)")
        startIfNeeded(null)
        println("nacos localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}
