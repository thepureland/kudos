package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object PostgresTestContainer {

    private const val DATABASE = "test"

    val CONTAINER = GenericContainer(DockerImageName.parse("postgres:15.5"))
        .withEnv("POSTGRES_DB", DATABASE)
        .withEnv("POSTGRES_USER", "postgres")
        .withEnv("POSTGRES_PASSWORD", "postgres")
        .withExposedPorts(5432)

    fun properties(registry: DynamicPropertyRegistry) {
        // 本来通过@Testcontainers就会自动启动容器，但这里手动启动它，原因为：
        // 确保properties()执行时，容器已经启动完毕，以便此时获取容器的一些变量，如端口
        CONTAINER.start()

        registry.add("spring.datasource.dynamic.datasource.postgres.url") {
            "jdbc:postgresql://localhost:${CONTAINER.firstMappedPort}/${DATABASE}"
        }
    }

}