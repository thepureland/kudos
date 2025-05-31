package io.kudos.test.container

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

    const val IMAGE_NAME_REDIS = "minio/minio:RELEASE.2025-04-22T22-12-26Z"

    val CONTAINER = GenericContainer<Nothing>(IMAGE_NAME_REDIS).apply {
        withEnv("MINIO_ROOT_USER", "admin")
        withEnv("MINIO_ROOT_PASSWORD", "12345678")
        withExposedPorts(9000)
        withCommand("server", "/data")
        withCopyFileToContainer(
            MountableFile.forClasspathResource("minio/minio.png"),
            "/docs/0/minio.png"
        )
        // 等待 HTTP 200 响应，路径可选 /minio/health/ready 或 /minio/health/live
        waitingFor(
            Wait.forHttp("/minio/health/ready")
                .forPort(9000)
                .withStartupTimeout(Duration.ofMinutes(1))
        )
    }

    fun start(registry: DynamicPropertyRegistry?): GenericContainer<*> {
        println(">>>>>>>>>>>>>>>>>>>> Starting MinIO container...")
        CONTAINER.start()
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> MinIO container started.")
        return CONTAINER
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        val endPoint = "http://${CONTAINER.host}:${CONTAINER.firstMappedPort}"
        registry.add("kudos.ability.file.minio.endpoint") { endPoint }
        registry.add("kudos.ability.file.minio.public-endpoint") { endPoint }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("minio localhost port: " + CONTAINER.firstMappedPort)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}