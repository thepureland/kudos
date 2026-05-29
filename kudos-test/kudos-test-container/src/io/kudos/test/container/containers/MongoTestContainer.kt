package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * Mongo test container.
 *
 * Uses [GenericContainer] rather than the dedicated [org.testcontainers.containers.MongoDBContainer]
 * — the latter forces replica-set mode (rs.initiate at startup) which is overkill for unit tests
 * and roughly triples the container boot time. The plain Mongo image runs as a standalone server
 * which is enough for [org.springframework.data.mongodb.core.MongoTemplate] CRUD coverage.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object MongoTestContainer {

    const val IMAGE_NAME_MONGO = "mongo:7.0.20"

    const val LABEL = "Mongo"

    private val CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_MONGO))
        .withExposedPorts(27017)
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)

    /**
     * Starts the container (if needed).
     *
     * Mirrors [RedisTestContainer.startIfNeeded] — a single Mongo container is shared across the
     * test batch; the harness registers a JVM shutdown hook so the container stops at the end.
     * Use `@EnabledIfDockerInstalled` on test classes to skip them on machines without Docker.
     *
     * @param registry Spring's dynamic property registry, used to register or override `spring.data.mongodb.*`
     * @return the running container instance
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return TestContainerCrossProcessLock.run(MongoTestContainer::class.java, "mongo") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Registers the actual Mongo container host/port under `spring.data.mongodb.*`. Uses the `uri`
     * shape (`mongodb://host:port/db`) rather than the legacy `host` + `port` + `database` triple:
     * Spring Boot's `MongoConnectionDetails` consults `uri` first and the host/port path only
     * kicks in when `uri` is absent — registering both is fine but redundant.
     *
     * @param registry the Spring dynamic property registry
     * @param runningContainer the running container
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer: Container) {
        val firstPort = runningContainer.ports.first()
        val host = requireNotNull(firstPort.ip) { "container port ip is null" }
        val port = requireNotNull(firstPort.publicPort) { "container publicPort is null" }
        val database = "kudos-test"

        val uri = "mongodb://$host:$port/$database"
        registry.add("spring.data.mongodb.host") { host }
        registry.add("spring.data.mongodb.port") { port }
        registry.add("spring.data.mongodb.database") { database }
        registry.add("spring.data.mongodb.uri") { uri }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Mongo")
        startIfNeeded(null)
        println("mongo localhost port: ${CONTAINER.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }
}
