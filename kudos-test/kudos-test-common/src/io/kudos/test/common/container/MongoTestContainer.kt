package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName


/**
 * mongo测试容器
 *
 * @author K
 * @since 1.0.0
 */
object MongoTestContainer {

    const val IMAGE_NAME = "mongo:7.0.4"

    const val USERNAME = "root"
    const val PASSWORD = "123456"

    private var CONTAINER = GenericContainer(DockerImageName.parse(IMAGE_NAME))
        .withExposedPorts(27017)
        .withEnv("MONGO_INITDB_ROOT_USERNAME", USERNAME)
        .withEnv("MONGO_INITDB_ROOT_PASSWORD", PASSWORD)

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val port = CONTAINER.firstMappedPort

        // 数据源 db1
        registry.add("soul.ability.mongo.dynamic.datasource.db1.ports") { port }

        // 数据源 db2
        registry.add("soul.ability.mongo.dynamic.datasource.db2.ports") { port }

        // 数据源 3
        val uri = "mongodb://${USERNAME}:${PASSWORD}@localhost:${port}/db3?authSource=admin&ssl=false"
        registry.add("soul.ability.mongo.dynamic.datasource.3.uri") { uri }
    }

    fun getContainer() = CONTAINER

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("MongoDb localhost port: ${CONTAINER.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}