package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration


/**
 * ollama测试容器
 *
 * 注：使用nomic-embed-text模型
 *
 * @author K
 * @since 1.0.0
 */
object OllamaTestContainer {

    private const val IMAGE_NAME = "ollama/ollama:0.13.5"

    const val PORT = 11434

    const val CONTAINER_PORT = 11434

    const val LABEL = "Ollama"

    private val container = GenericContainer(IMAGE_NAME).apply {
        // 絕對路徑，並確保目錄存在
        val hostDir: Path = Path.of(System.getProperty("user.home"), ".cache", "ollama-tc")
        Files.createDirectories(hostDir)

        withFileSystemBind(
            hostDir.toAbsolutePath().toString(),
            "/root/.ollama"
        )

        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        // 让 Ollama 监听 0.0.0.0（容器内可被映射端口访问）
        withEnv("OLLAMA_HOST", "0.0.0.0:$CONTAINER_PORT")

        waitingFor(
            Wait.forHttp("/api/tags")
                .forPort(PORT)
                .forStatusCode(200)
        )
        withStartupTimeout(Duration.ofMinutes(3))

        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * 拉取模型
     */
    private fun pullModel(model: String) {
        println("Start pulling model: $model ...")
        val time = System.currentTimeMillis()
        val r = container.execInContainer("ollama", "pull", model)
        check(r.exitCode == 0) { "ollama pull $model failed: ${r.stderr}\n${r.stdout}" }
        println("Finish pulling model: $model in ${System.currentTimeMillis() - time}ms")
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
            val running = TestContainerKit.isContainerRunning(LABEL)
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (!running) {
                pullModel("nomic-embed-text")
            }
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {

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
        println("Ollama ${container.host} port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}