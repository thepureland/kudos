package io.kudos.test.container

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * smtp测试容器
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
object SmtpTestContainer {

    const val IMAGE_NAME: String = "namshi/smtp:latest"

    val container = GenericContainer(DockerImageName.parse(IMAGE_NAME))
        .withExposedPorts(25)

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        container.start()
        if (registry != null) {
            registerProperties(registry)
        }
        return container
    }

    private fun registerProperties(registry: DynamicPropertyRegistry?) {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("smtp localhost port: " + container.firstMappedPort)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}
