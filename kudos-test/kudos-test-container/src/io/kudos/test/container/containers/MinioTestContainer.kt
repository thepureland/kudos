package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.time.Duration


/**
 * minio测试容器
 *
 * @author K
 * @since 1.0.0
 */
object MinioTestContainer {

    const val IMAGE_NAME = "minio/minio:RELEASE.2025-04-22T22-12-26Z"

    const val PORT = 9000

    const val LABEL = "Minio"

    val CONTAINER = GenericContainer<Nothing>(IMAGE_NAME).apply {
        withEnv("MINIO_ROOT_USER", "admin")
        withEnv("MINIO_ROOT_PASSWORD", "12345678")
        withExposedPorts(PORT)
        withCommand("server", "/data")
        withCopyFileToContainer(
            MountableFile.forClasspathResource("minio/minio.png"),
            "/docs/0/minio.png"
        )
        // 等待 HTTP 200 响应，路径可选 /minio/health/ready 或 /minio/health/live
        waitingFor(
            Wait.forHttp("/minio/health/ready")
                .forPort(PORT)
                .withStartupTimeout(Duration.ofMinutes(1))
        )
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

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

        val endPoint = "http://$host:$port"
        registry.add("kudos.ability.file.minio.endpoint") { endPoint }
        registry.add("kudos.ability.file.minio.public-endpoint") { endPoint }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        println("minio localhost port: " + CONTAINER.firstMappedPort)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}