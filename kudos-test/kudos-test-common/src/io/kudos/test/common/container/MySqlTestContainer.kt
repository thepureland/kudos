package io.kudos.test.common.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object MySqlTestContainer {

    val CONTAINER = GenericContainer(DockerImageName.parse("mysql:5.7.44"))
        .withEnv("MYSQL_DATABASE", "test")
        .withEnv("MYSQL_ROOT_PASSWORD", "mysql")
        .withExposedPorts(3306)

    fun properties(registry: DynamicPropertyRegistry) {
        // 本来通过@Testcontainers就会自动启动容器，但这里手动启动它，原因为：
        // 确保properties()执行时，容器已经启动完毕，以便此时获取容器的一些初始化变量
        CONTAINER.start()

        registry.add("spring.datasource.dynamic.datasource.mysql.url") {
            "jdbc:mysql://${CONTAINER.host}:${CONTAINER.firstMappedPort}/${CONTAINER.envMap["MYSQL_DATABASE"]}"
        }
        registry.add("spring.datasource.dynamic.datasource.mysql.username") { "root" }
        registry.add("spring.datasource.dynamic.datasource.mysql.password") { CONTAINER.envMap["MYSQL_ROOT_PASSWORD"] }
        registry.add("spring.datasource.dynamic.datasource.mysql.driver-class-name") { "com.mysql.jdbc.Driver" }
    }

}