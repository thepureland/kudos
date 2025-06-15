package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * redis测试容器
 *
 * @author K
 * @since 1.0.0
 */
object RedisTestContainer {

    const val IMAGE_NAME_REDIS = "redis:8.0"

    const val LABEL = "Redis"

    val CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_REDIS))
        .withExposedPorts(6379)
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)

    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER, this)
        if (registry != null) {
            registerProperties(registry, runningContainer)
        }
        return runningContainer
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        registry.add("kudos.ability.data.redis.redis-map.data.host") { host }
        registry.add("kudos.ability.data.redis.redis-map.data.port") { port }

        //waring: 以下没有重置的话,NettyWebSocketDistributedTest测试不通过
        registry.add("spring.data.redis.host") { host }
        registry.add("spring.data.redis.port") { port }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("redis localhost port: ${CONTAINER.firstMappedPort}")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
