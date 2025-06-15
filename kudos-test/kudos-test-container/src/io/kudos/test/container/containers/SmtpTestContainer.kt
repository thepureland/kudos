package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * smtp测试容器
 *
 * @author K
 * @since 1.0.0
 */
object SmtpTestContainer {

    const val IMAGE_NAME = "namshi/smtp:latest"

    const val LABEL = "Smtp"

    val container = GenericContainer(DockerImageName.parse(IMAGE_NAME)).apply {
        withExposedPorts(25)
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }


    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container, this)
        if (registry != null) {
            registerProperties(registry, runningContainer)
        }
        return runningContainer
    }

    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer : Container) {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("smtp localhost port: ${container.firstMappedPort}")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}
