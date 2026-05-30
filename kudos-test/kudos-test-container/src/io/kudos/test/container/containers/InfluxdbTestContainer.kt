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
 * InfluxDB 2.x test container.
 *
 * Uses [GenericContainer] + the official `influxdb:2.7-alpine` image with the
 * `DOCKER_INFLUXDB_INIT_MODE=setup` env config so the container starts up
 * fully provisioned (admin user + org + bucket + token) without the test
 * having to call `influx setup` itself. The fixed admin token below is OK
 * for testing — it's never exposed outside the container's lifecycle.
 *
 * Registers properties under `kudos.ability.tsdb.influxdb.{url,token,org,bucket}`,
 * so a test class using `@EnableKudosTest` + this container's `startIfNeeded`
 * gets a fully wired [com.influxdb.client.InfluxDBClient] bean.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object InfluxdbTestContainer {

    const val IMAGE_NAME_INFLUXDB = "influxdb:2.7-alpine"

    const val LABEL = "InfluxDB"

    const val ORG = "kudos-test-org"

    const val BUCKET = "kudos-test-bucket"

    // 32-char fixed token — InfluxDB 2.x rejects shorter ones. Test-only.
    const val ADMIN_TOKEN = "kudos-test-token-32-bytes-long-x"

    private val CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_INFLUXDB))
        .withExposedPorts(8086)
        .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
        .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
        .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "adminpassword")
        .withEnv("DOCKER_INFLUXDB_INIT_ORG", ORG)
        .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", BUCKET)
        .withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", ADMIN_TOKEN)
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)
        // /health returns 200 once setup completes; using it as the readiness gate is more
        // reliable than the default container-startup heuristic on slow CI.
        .waitingFor(Wait.forHttp("/health").forPort(8086))

    /**
     * Starts the container (if needed) and registers the InfluxDB connection properties under
     * `kudos.ability.tsdb.influxdb.*` so the autoconfig wires its `InfluxDBClient` against the
     * testcontainer.
     *
     * @param registry Spring's dynamic property registry; null when called from the manual
     *   `main()` driver below.
     * @return the running container instance
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(InfluxdbTestContainer::class.java, "influxdb") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer: Container) {
        val port = runningContainer.ports.firstOrNull { it.privatePort == 8086 }
            ?: error("InfluxDB container exposes no 8086 mapping")
        val host = requireNotNull(port.ip) { "container port ip is null" }
        val publicPort = requireNotNull(port.publicPort) { "container publicPort is null" }
        val url = "http://$host:$publicPort"

        registry.add("kudos.ability.tsdb.influxdb.url") { url }
        registry.add("kudos.ability.tsdb.influxdb.token") { ADMIN_TOKEN }
        registry.add("kudos.ability.tsdb.influxdb.org") { ORG }
        registry.add("kudos.ability.tsdb.influxdb.bucket") { BUCKET }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "InfluxDB")
        startIfNeeded(null)
        println("influxdb HTTP port: ${CONTAINER.getMappedPort(8086)}")
        println("admin token: $ADMIN_TOKEN")
        Thread.sleep(Long.MAX_VALUE)
    }
}
