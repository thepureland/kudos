package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.net.IpKit
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.slf4j.LoggerFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * seata-server test container.
 *
 * @author K
 * @since 1.0.0
 */
object SeataTestContainer {

    private const val IMAGE_NAME = "apache/seata-server:2.5.0"

    /** Seata 2.5's in-container service-port defaults to 8091 (not 28091); the externally mapped host port is still called SERVICE_PORT. */
    private const val CONTAINER_SERVICE_PORT = 8091

    const val SERVICE_PORT = 28091

    const val LABEL = "Seata"

    /** Must match the host port used by Seata's Nacos in NacosTestContainer. */
    private const val NACOS_FOR_SEATA_PORT = 38848

    private const val SEATA_SERVICE_NAME = "seata-server"

    /** Polling interval and maximum wait time; waits until Seata is registered in Nacos before returning, to avoid the client "no available service found in cluster" error. */
    private const val POLL_INTERVAL_MS = 800L
    private const val MAX_WAIT_MS = 30_000L

    private lateinit var runningNacosContainer : Container

    private val CONTAINER = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(CONTAINER_SERVICE_PORT)
        bindingPort(Pair(SERVICE_PORT, CONTAINER_SERVICE_PORT))
        withEnv("SEATA_IP", IpKit.getLocalIp())
        withNetwork(TestContainerKit.DEFAULT_DOCKER_NETWORK)
        withClasspathResourceMapping(
            "seata/seata-server.yml",
            "/seata-server/resources/application.yml",
            BindMode.READ_ONLY
        )
        // The Seata 2.5 image has no netcat and no HTTP web endpoint (console was split into a standalone namingserver module) →
        // forListeningPort() and forHttp() are both unreliable. Switch to log matching: treat Seata as ready when it logs "service listen port".
        waitingFor(Wait.forLogMessage(".*Server started, service listen port.*\\n", 1))
        // Pipe in-container stdout/stderr to the test log (prefix "seata-server") — once the container is reaped by Ryuk the logs are gone,
        // and the root cause of integration failures (Spring startup stack traces, registration failure reasons) is only visible via the log consumer.
        withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("seata-server")).withSeparateOutputStreams())
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * Starts the containers (if needed). Starts both Nacos and Seata and puts them on the same Docker network so they can address each other by alias.
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
        runningNacosContainer = NacosTestContainer.startNacosForSeataIfNeeded(registry)

        return TestContainerCrossProcessLock.run(SeataTestContainer::class.java, "seata") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
                waitForSeataRegisteredInNacos()
            }
            runningContainer
        }
    }

    /**
     * The test JVM runs on the host; if Nacos discovery is used, it returns the address Seata registered (possibly the container's internal IP), which the host cannot reach.
     * Override to file + direct connection to the host-mapped port — only the test side connects to TC directly; the Seata server still uses Nacos + the shared network.
     */
    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
        if (registry == null) return
        registry.add("seata.registry.type") { "file" }
        registry.add("seata.service.default.grouplist") { "127.0.0.1:$SERVICE_PORT" }
    }

    /**
     * Polls Nacos until seata-server has been registered (at least one instance) before returning, to avoid "no available service found in cluster" when the client starts up.
     */
    private fun waitForSeataRegisteredInNacos() {
        val url = "http://127.0.0.1:$NACOS_FOR_SEATA_PORT/nacos/v1/ns/instance/list?serviceName=$SEATA_SERVICE_NAME"
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build()
        val deadline = System.currentTimeMillis() + MAX_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200 && response.body().contains("\"hosts\":[{")) {
                    return
                }
            } catch (_: Exception) {
                // Nacos or Seata is not ready yet; keep polling
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw IllegalStateException(
            "Seata server did not register to Nacos within ${MAX_WAIT_MS}ms. Check Nacos($NACOS_FOR_SEATA_PORT) and Seata container."
        )
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        // Seata depends on Nacos: the manual startup entry point cleans up historical containers on both sides to avoid interference from old instances.
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Seata")
        ManualTestContainerMainSupport.removeExistingContainers(NacosTestContainer.LABEL_NACOS_FOR_SEATA, "Nacos (for Seata)")
        startIfNeeded(null)
        println("nacos localhost web-port: ${runningNacosContainer.ports.first().publicPort}")
        println("seata localhost service-port: $SERVICE_PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}
