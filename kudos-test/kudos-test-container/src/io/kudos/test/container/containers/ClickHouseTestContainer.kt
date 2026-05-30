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
 * ClickHouse test container.
 *
 * Single standalone server (no cluster / no ZooKeeper) — production deployments use
 * `ReplicatedMergeTree` + `Distributed` engines, but for unit-test purposes a vanilla
 * `MergeTree` against one standalone node is enough to exercise the INSERT / SELECT round trip.
 *
 * Uses [GenericContainer] rather than testcontainers' dedicated `ClickHouseContainer` to keep
 * setup uniform with the other kudos containers (Redis / Mongo) and dodge that class's older API.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object ClickHouseTestContainer {

    const val IMAGE_NAME_CLICKHOUSE = "clickhouse/clickhouse-server:24.8-alpine"

    const val LABEL = "ClickHouse"

    const val DATABASE = "kudos_test"

    const val USERNAME = "default"

    const val PASSWORD = ""

    private val CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_CLICKHOUSE))
        .withExposedPorts(8123, 9000)
        .withEnv("CLICKHOUSE_DB", DATABASE)
        .withEnv("CLICKHOUSE_SKIP_USER_SETUP", "1")
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)
        // The HTTP port (8123) is what the JDBC driver talks to; wait for the health endpoint
        // rather than the default startup heuristic, which sometimes races with the server.
        .waitingFor(Wait.forHttp("/ping").forPort(8123))

    /**
     * Starts the container (if needed).
     *
     * @param registry Spring's dynamic property registry; the test-side JDBC URL gets registered
     *   under `kudos.test.clickhouse.*` so tests inject it where they actually need it (we don't
     *   write to `spring.datasource.*` to keep ClickHouse from conflicting with the app's
     *   business RDB).
     * @return the running container instance
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(ClickHouseTestContainer::class.java, "clickhouse") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer: Container) {
        // ports[0] = 8123 (HTTP), ports[1] = 9000 (native protocol). The kudos audit-clickhouse
        // module talks to ClickHouse over JDBC-on-HTTP, so the HTTP-side public port is the one
        // we route to.
        val httpPort = runningContainer.ports.first { it.privatePort == 8123 }
        val host = requireNotNull(httpPort.ip) { "container HTTP port ip is null" }
        val port = requireNotNull(httpPort.publicPort) { "container HTTP publicPort is null" }
        val jdbcUrl = "jdbc:clickhouse://$host:$port/$DATABASE"

        registry.add("kudos.test.clickhouse.jdbc-url") { jdbcUrl }
        registry.add("kudos.test.clickhouse.username") { USERNAME }
        registry.add("kudos.test.clickhouse.password") { PASSWORD }
        registry.add("kudos.test.clickhouse.database") { DATABASE }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "ClickHouse")
        startIfNeeded(null)
        println("clickhouse HTTP port: ${CONTAINER.getMappedPort(8123)}")
        Thread.sleep(Long.MAX_VALUE)
    }
}
