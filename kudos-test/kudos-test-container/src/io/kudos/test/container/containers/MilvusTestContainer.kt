package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.io.PathKit
import io.kudos.test.container.kit.TestContainerKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

/**
 * kafka测试容器
 *
 * @author ChatGpt
 * @author K
 * @since 1.0.0
 */
object MilvusTestContainer {

    private const val PORT = 19530

    private const val HTTP_PORT = 9091

    private const val LABEL = "Milvus"

    private val composeFile = File("${PathKit.getResourcePath("milvus/docker-compose-milvus.yml")}")

    private val compose = ComposeContainer(composeFile).apply {
        // Milvus gRPC 19530
        withExposedService(
            "milvus-1",
            PORT,
            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5))
        )
        // Milvus HTTP 9091（healthz）
        withExposedService(
            "milvus-1",
            HTTP_PORT,
            Wait.forHttp("/healthz").forPort(HTTP_PORT).forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(5))
        )
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
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container? {
        synchronized(this) {
            val started = TestContainerKit.isContainerRunning(LABEL)
            if (!started) {
                compose.start()
                println("Milvus compose started.")
            }

            val runningContainer = TestContainerKit.getRunningContainer(LABEL)
            if (registry != null && runningContainer != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        registry.add("spring.ai.vectorstore.milvus.host") { "$host" }

        val port = runningContainer.ports.first().publicPort
        registry.add("spring.ai.vectorstore.milvus.port") { "$port" }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        val host = compose.getServiceHost("milvus-1", PORT)
        val port = compose.getServicePort("milvus-1", PORT)
        println("Milvus endpoint: $host:$port")
        Thread.sleep(Long.MAX_VALUE)
    }

}
