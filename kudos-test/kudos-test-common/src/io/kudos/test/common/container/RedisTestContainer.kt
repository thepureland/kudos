package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object RedisTestContainer {

    val CONTAINER = GenericContainer(DockerImageName.parse("redis:7.2.2")).withExposedPorts(6379)

    fun properties(registry: DynamicPropertyRegistry) {
        // 本来通过@Testcontainers就会自动启动容器，但这里手动启动它，原因为：
        // 确保properties()执行时，容器已经启动完毕，以便此时获取容器的一些初始化变量
        CONTAINER.start()

        registry.add("kudos.ability.data.redis.redis-map.data.host") { CONTAINER.host }
        registry.add("kudos.ability.data.redis.redis-map.data.port") { CONTAINER.firstMappedPort }
    }

}