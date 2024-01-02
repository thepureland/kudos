package io.kudos.test.common.container

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

    const val IMAGE_NAME_REDIS = "redis:7.2.2"

    private var CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME_REDIS))
        .withExposedPorts(6379)

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("kudos.ability.data.redis.redis-map.data.host") { CONTAINER.host }
        registry.add("kudos.ability.data.redis.redis-map.data.port") { CONTAINER.firstMappedPort }
    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("redis localhost port: ${CONTAINER.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}