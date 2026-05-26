package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * redis test container.
 *
 * @author K
 * @since 1.0.0
 */
object RedisTestContainer {

    const val IMAGE_NAME_REDIS = "redis:8.6.0-alpine"

    const val LABEL = "Redis"

    private val CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_REDIS))
        .withExposedPorts(6379)
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
        return TestContainerCrossProcessLock.run(RedisTestContainer::class.java, "redis") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * Registers the actual Redis container host/port to two property groups:
     * 1. `kudos.ability.data.redis.redis-map.*` — config used by kudos' own Redis abstraction layer
     * 2. `spring.data.redis.*` — Spring Data Redis (read directly by low-level components such as NettyWebSocketDistributedTest)
     *
     * Both groups must be set — writing only (1) causes NettyWebSocketDistributedTest to read the wrong address via the original spring config.
     *
     * @param registry the Spring dynamic property registry
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val firstPort = runningContainer.ports.first()
        val host = requireNotNull(firstPort.ip) { "container port ip is null" }
        val port = requireNotNull(firstPort.publicPort) { "container publicPort is null" }

        registry.add("kudos.ability.data.redis.redis-map.data.host") { host }
        registry.add("kudos.ability.data.redis.redis-map.data.port") { port }

        // warning: without resetting the following, NettyWebSocketDistributedTest will fail
        registry.add("spring.data.redis.host") { host }
        registry.add("spring.data.redis.port") { port }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Redis")
        startIfNeeded(null)
        println("redis localhost port: ${CONTAINER.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}
