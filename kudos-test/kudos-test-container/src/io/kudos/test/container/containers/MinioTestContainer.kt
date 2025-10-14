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

    const val IMAGE_NAME = "minio/minio:RELEASE.2025-09-07T16-13-09Z"

    const val PORT = 9000

    const val LABEL = "Minio"

    private val CONTAINER = GenericContainer<Nothing>(IMAGE_NAME).apply {
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
                .forStatusCode(200)
        )
        withStartupTimeout(Duration.ofMinutes(1))
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没启动时想忽略测试用例，可以用@EnabledIfDockerAvailable
     * 来代替@Testcontainers(disabledWithoutDocker = true)
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val endPoint = "http://$host:$port"
        registry.add("kudos.ability.file.minio.endpoint") { endPoint }
        registry.add("kudos.ability.file.minio.public-endpoint") { endPoint }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("minio localhost port: " + CONTAINER.firstMappedPort)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}