package io.kudos.test.container

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

    const val IMAGE_NAME_REDIS: String = "redis:8.0"

    val CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_REDIS))
        .withExposedPorts(6379)

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("kudos.ability.data.redis.redis-map.data.host", CONTAINER::getHost)
        registry.add("kudos.ability.data.redis.redis-map.data.port", CONTAINER::getFirstMappedPort)

        //waring: 以下没有重置的话,NettyWebSocketDistributedTest测试不通过
        registry.add("spring.data.redis.host", CONTAINER::getHost)
        registry.add("spring.data.redis.port", CONTAINER::getFirstMappedPort)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("redis localhost port: " + CONTAINER.getFirstMappedPort())
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}
