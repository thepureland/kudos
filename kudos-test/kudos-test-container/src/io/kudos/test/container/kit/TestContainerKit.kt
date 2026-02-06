package io.kudos.test.container.kit

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.system.measureTimeMillis

/**
 * test-container工具类
 *
 * @author K
 * @since 1.0.0
 */
object TestContainerKit {

    const val LABEL_KEY = "kudos-test-container"

    val DEFAULT_DOCKER_NETWORK: Network = Network.newNetwork()

    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没安装时想忽略测试用例，可以用@EnabledIfDockerInstalled
     *
     * @param label 容器的label的值(key必须为label)
     * @param container 容器
     * @return 容器对象
     */
    fun startContainerIfNeeded(
        label: String,
        container: GenericContainer<*>,
    ): Container {
        var runningContainer = getRunningContainer(label)
        if (runningContainer == null) {
            startContainer(container, label)
            runningContainer = getRunningContainer(label)
        } else {
            println("############  $label container has already been started.")
        }
        return runningContainer!!
    }

    /**
     * 容器是否在运行中
     *
     * 注：container.isRunning只是判断当前jvm中的状态值，而该方法可以判断在docker中运行着。
     *
     * @param label 容器的label的值(key必须为label)
     * @return true: 运行中，false：未运行
     */
    fun isContainerRunning(label: String): Boolean = getRunningContainer(label) != null

    /**
     * 服务是否在运行中
     *
     * @param compose 由compose.yml跑的容器实例
     * @param serviceInstanceName 服务实例名
     * @return true: 运行中，false：未运行
     */
    fun isServiceRunning(compose: ComposeContainer, serviceInstanceName: String): Boolean {
        val opt = compose.getContainerByServiceName(serviceInstanceName)
        return opt.isPresent && opt.get().isRunning
    }

    /**
     * 根据label获取对应的容器
     *
     * @param label 容器的label的值(key必须为label)
     * @return 容器对象，不存在返回null
     */
    fun getRunningContainer(label: String): Container? {
        DockerKit.ensureDockerRunning()
        val dockerClient = DockerClientFactory.lazyClient()
        val containers = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withLabelFilter(mapOf(LABEL_KEY to label))
            .exec()
        return containers.firstOrNull()
    }

    /**
     * 启动容器
     *
     * @param container 容器
     * @param label 容器助记名
     */
    fun startContainer(container: GenericContainer<*>, label: String) {
        println(">>>>>>>>>>>>>>>>>>>> Starting $label container...")
        val time = measureTimeMillis { container.start() }
        println("<<<<<<<<<<<<<<<<<<<< $label container started in $time ms.")

        addShutdownHook(container, label)
    }

    /**
     * 注册 JVM 关闭钩子，自动停止容器
     *
     * 可以让批量测试结束时才停止容器，而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解
     *
     * @param container 容器
     * @param label 容器助记名
     */
    fun addShutdownHook(container: GenericContainer<*>, label: String) {
        Runtime.getRuntime().addShutdownHook(Thread {
            println(">>>>>>>>>>>>>>>>>>>> Stopping $label container...")
            val time = measureTimeMillis { container.stop() }
            println("<<<<<<<<<<<<<<<<<<<< $label container stopped in $time ms.")
        })
    }

    /**
     * 直接在容器内执行命令
     *
     * @param container 容器对象
     * @param command 要执行的命令及其参数
     * @return 执行结果，包含退出码、stdout 和 stderr
     */
    private fun execInContainer(container: GenericContainer<*>, vararg command: String): ExecResult {
        val result = container.execInContainer(*command)
        return ExecResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr
        )
    }

    /**
     * 通过 Docker Java API 在容器内执行命令
     *
     * @param container Docker Java API 的 Container 对象
     * @param command 要执行的命令及其参数
     * @return 执行结果，包含退出码、stdout 和 stderr
     */
    fun execInContainer(container: Container, vararg command: String): ExecResult {
        val dockerClient = getDockerClient()
        val containerId = container.id

        // 创建 exec 命令
        val execCreateCmd = dockerClient.execCreateCmd(containerId)
            .withCmd(*command)
            .withAttachStdout(true)
            .withAttachStderr(true)

        val execCreateResponse: ExecCreateCmdResponse = execCreateCmd.exec()
        val execId = execCreateResponse.id

        // 执行命令并获取输出
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                try {
                    when (frame.streamType) {
                        StreamType.STDOUT -> stdout.write(frame.payload)
                        StreamType.STDERR -> stderr.write(frame.payload)
                        else -> {
                            // 其他类型（如 RAW）忽略或根据实际情况处理
                        }
                    }
                } catch (e: IOException) {
                    throw RuntimeException("Failed to write frame output", e)
                }
            }
        }

        dockerClient.execStartCmd(execId).exec(callback).awaitCompletion()

        // 获取退出码
        val inspectExecResponse = dockerClient.inspectExecCmd(execId).exec()
        val exitCode = inspectExecResponse.exitCodeLong ?: -1

        return ExecResult(
            exitCode = exitCode.toInt(),
            stdout = stdout.toString(),
            stderr = stderr.toString()
        )
    }

    /**
     * 执行结果数据类
     */
    data class ExecResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    /**
     * 获取 DockerClient 实例（使用 Testcontainers 的 DockerClientFactory）
     */
    fun getDockerClient(): DockerClient {
        return DockerClientFactory.instance().client()
    }


}