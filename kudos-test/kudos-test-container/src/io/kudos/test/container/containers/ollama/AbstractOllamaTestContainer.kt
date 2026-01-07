package io.kudos.test.container.containers.ollama

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
 * ollama测试容器抽象類
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractOllamaTestContainer(
    val imageName: String,
    val port: Int,
    val label: String
) {

    private val containerPort = 11434

    protected val container = GenericContainer(imageName).apply {
        // 絕對路徑，並確保目錄存在
        val hostDir: Path = Path.of(System.getProperty("user.home"), ".cache", "ollama-tc")
        Files.createDirectories(hostDir)

        withFileSystemBind(
            hostDir.toAbsolutePath().toString(),
            "/root/.ollama"
        )

        withExposedPorts(containerPort)
        bindingPort(Pair(port, containerPort))
        // 让 Ollama 监听 0.0.0.0（容器内可被映射端口访问）
        withEnv("OLLAMA_HOST", "0.0.0.0:$containerPort")

        waitingFor(
            Wait.forHttp("/api/tags")
                .forPort(port)
                .forStatusCode(200)
        )
        withStartupTimeout(Duration.ofMinutes(3))

        withLabel(TestContainerKit.LABEL_KEY, label)
    }

    /**
     * 拉取模型
     */
    private fun pullModelIfAbsent(model: OllamaModel) {
        // 1) 先检查是否已存在
        val list = container.execInContainer("ollama", "list")
        check(list.exitCode == 0) { "ollama list failed: ${list.stderr}\n${list.stdout}" }

        val existingModels: Set<String> = list.stdout
            .lineSequence()
            .drop(1) // 跳过表头：NAME ID SIZE MODIFIED
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // NAME 通常是第一列，可能是 all-minilm:l6-v2 这种
                line.split(Regex("\\s+"), limit = 2).first()
            }
            .toSet()

        if (model.modelName in existingModels) {
            println("Model already exists: $model, skip pulling.")
            return
        }

        // 2) 不存在才 pull
        println("Start pulling model: $model ...")
        val time = System.currentTimeMillis()
        val r = container.execInContainer("ollama", "pull", model.modelName)
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
     * @param model ollama支持的模型
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?, model: OllamaModel): Container {
        synchronized(this) {
            val running = TestContainerKit.isContainerRunning(label)
            val runningContainer = TestContainerKit.startContainerIfNeeded(label, container)
            if (!running) {
                pullModelIfAbsent(model)
            }
            if (registry != null) {
                registerProperties(registry, runningContainer, model)
            }
            return runningContainer
        }
    }

    protected fun registerProperties(
        registry: DynamicPropertyRegistry,
        runningContainer: Container,
        model: OllamaModel
    ) {
        registry.add("spring.ai.model.embedding") { "ollama" }
        registry.add("spring.ai.ollama.embedding.options.model") { model.modelName }
        registry.add("spring.ai.ollama.base-url") { "http://127.0.0.1:$port" }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(label)

}