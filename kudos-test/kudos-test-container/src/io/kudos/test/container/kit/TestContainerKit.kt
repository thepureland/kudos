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
import org.testcontainers.utility.TestcontainersConfiguration
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * test-container工具类
 *
 * 在 [SHARED_LIFECYCLE_ENABLED] 启用时（默认启用），多个 JVM 通过本机文件租约共享同一个 docker 容器：
 * - 仅当不存在运行中的同 label 容器时才会启动
 * - 每个使用容器的 JVM 各自登记一个租约文件
 * - JVM 关闭钩子只释放本进程的租约；只有最后一个存活租约离开时才真正停止容器
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object TestContainerKit {

    const val LABEL_KEY = "kudos-test-container"

    /**
     * 是否启用跨 JVM 共用生命周期。默认启用；若需回到旧行为，可设为 `false`。
     */
    const val SHARED_LIFECYCLE_ENABLED = "kudos.testcontainer.shared-lifecycle.enabled"

    val DEFAULT_DOCKER_NETWORK: Network = Network.newNetwork()

    private val LEASE_ROOT: Path =
        Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-leases")
    private val PID: Long = ProcessHandle.current().pid()
    private val PROCESS_MARK: Long = System.nanoTime()
    private val REGISTERED_LABELS: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     *
     * 多个 JVM 同时连到同一个容器时，会以本机租约追踪使用者；先退出的 JVM 不会停止仍被其它 JVM 使用中的容器，
     * 只有最后一个存活租约释放后才会停止容器。
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
            addShutdownHook(container, label)
        }
        return requireNotNull(runningContainer) { "runningContainer is null for label $label" }
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
    fun isServiceRunning(compose: ComposeContainer, serviceInstanceName: String): Boolean =
        compose.getContainerByServiceName(serviceInstanceName)
            .map { it.isRunning }
            .orElse(false)

    /**
     * 根据label获取对应的运行中容器
     *
     * 仅返回真正运行中的容器（`withShowAll(false)`）；处于 exited 状态的残留容器不会被误当作"已就绪"返回，
     * 从而触发后续按需启动。
     *
     * @param label 容器的label的值(key必须为label)
     * @return 容器对象，不存在返回null
     */
    fun getRunningContainer(label: String): Container? {
        DockerKit.ensureDockerRunning()
        val dockerClient = DockerClientFactory.lazyClient()
        val containers = dockerClient.listContainersCmd()
            .withShowAll(false)
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
        prepareSharedReusableContainer(container)
        val time = measureTimeMillis { container.start() }
        println("<<<<<<<<<<<<<<<<<<<< $label container started in $time ms.")

        addShutdownHook(container, label)
    }

    /**
     * 注册 JVM 关闭钩子，自动停止容器
     *
     * 可以让批量测试结束时才停止容器，而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解
     *
     * 启用共用生命周期时，关闭钩子只释放当前 JVM 的租约；若还有其它存活 JVM 租约，容器会继续保留。
     *
     * @param container 容器
     * @param label 容器助记名
     */
    fun addShutdownHook(container: GenericContainer<*>, label: String) {
        if (!isSharedLifecycleEnabled()) {
            Runtime.getRuntime().addShutdownHook(Thread {
                println(">>>>>>>>>>>>>>>>>>>> Stopping $label container...")
                val time = measureTimeMillis { container.stop() }
                println("<<<<<<<<<<<<<<<<<<<< $label container stopped in $time ms.")
            })
            return
        }
        registerLease(label)
        if (!REGISTERED_LABELS.add(label)) {
            return
        }
        Runtime.getRuntime().addShutdownHook(
            Thread({ releaseAndStopIfLast(label) }, "testcontainer-$label")
        )
    }

    /**
     * 共用容器必须避开 Testcontainers/Ryuk 的单一 JVM session 清理，
     * 否则第一个启动者离线时仍会被 Ryuk 回收。
     */
    private fun prepareSharedReusableContainer(container: GenericContainer<*>) {
        if (!isSharedLifecycleEnabled()) return
        enableTestcontainersReuseInMemory()
        container.withReuse(true)
    }

    /**
     * 只在当前 JVM 内存中打开 reuse，不写入用户家目录的 `~/.testcontainers.properties`。
     */
    private fun enableTestcontainersReuseInMemory() {
        try {
            val configuration = TestcontainersConfiguration.getInstance()
            val userPropertiesField = TestcontainersConfiguration::class.java.getDeclaredField("userProperties")
            userPropertiesField.isAccessible = true
            val userProperties = userPropertiesField.get(configuration) as Properties
            userProperties.setProperty("testcontainers.reuse.enable", "true")
        } catch (e: ReflectiveOperationException) {
            throw IllegalStateException("Failed to enable Testcontainers reuse for shared containers", e)
        }
    }

    /**
     * 释放当前 JVM 的租约，并在确认没有其它存活租约时停止对应 label 的容器。
     */
    private fun releaseAndStopIfLast(label: String) {
        withLabelLock(label) {
            releaseLease(label)
            val activeLeases = pruneAndCountActiveLeases(label)
            if (activeLeases > 0) {
                println("############  $label container still used by $activeLeases JVM(s), skip stop.")
                return@withLabelLock
            }
            stopContainerByLabel(label)
        }
    }

    /**
     * 直接通过 Docker label 查找并停止容器，避免依赖当前 JVM 是否持有原始 [GenericContainer] 实例。
     */
    private fun stopContainerByLabel(label: String) {
        DockerKit.ensureDockerRunning()
        val dockerClient = DockerClientFactory.lazyClient()
        val containers = dockerClient.listContainersCmd()
            .withShowAll(false)
            .withLabelFilter(mapOf(LABEL_KEY to label))
            .exec()
        if (containers.isEmpty()) return
        for (container in containers) {
            val id = container.id
            println(">>>>>>>>>>>>>>>>>>>> Stopping $label container...")
            val time = measureTimeMillis {
                dockerClient.stopContainerCmd(id).exec()
                dockerClient.removeContainerCmd(id).withForce(true).withRemoveVolumes(true).exec()
            }
            println("<<<<<<<<<<<<<<<<<<<< $label container stopped in $time ms.")
        }
    }

    /**
     * 租约文件名带入 pid 与进程内唯一值，避免同一 pid 被操作系统重用时误删新进程租约。
     */
    private fun registerLease(label: String) {
        withLabelLock(label) {
            try {
                Files.createDirectories(leaseDir(label))
                Files.writeString(
                    leaseFile(label),
                    PID.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                )
                pruneAndCountActiveLeases(label)
            } catch (e: IOException) {
                throw IllegalStateException("Failed to register testcontainer lease for $label", e)
            }
        }
    }

    /**
     * 删除当前 JVM 对指定容器 label 的租约文件。
     */
    private fun releaseLease(label: String) {
        try {
            Files.deleteIfExists(leaseFile(label))
        } catch (e: IOException) {
            System.err.println("Failed to release testcontainer lease for $label: ${e.message}")
        }
    }

    /**
     * 清掉已失效的租约文件，并返回仍存活的 JVM 租约数。
     */
    private fun pruneAndCountActiveLeases(label: String): Int {
        val dir = leaseDir(label)
        if (!Files.isDirectory(dir)) return 0
        var active = 0
        try {
            Files.list(dir).use { stream ->
                for (file in stream.toList()) {
                    val pid = readLeasePid(file)
                    if (pid != null && isProcessAlive(pid)) {
                        active++
                    } else {
                        Files.deleteIfExists(file)
                    }
                }
            }
        } catch (e: IOException) {
            throw IllegalStateException("Failed to inspect testcontainer leases for $label", e)
        }
        return active
    }

    /**
     * 从租约文件读取 pid；文件损坏或内容无效时视为失效租约。
     */
    private fun readLeasePid(file: Path): Long? {
        return try {
            val value = Files.readString(file, StandardCharsets.UTF_8).trim()
            if (value.isEmpty()) null else value.toLong()
        } catch (e: IOException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * 判断指定 pid 是否仍有存活进程。
     */
    private fun isProcessAlive(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    /**
     * 同一 label 的租约增删与最后停止判断必须互斥，避免两个 JVM 同时认定自己是最后使用者。
     */
    private fun withLabelLock(label: String, action: () -> Unit) {
        try {
            Files.createDirectories(LEASE_ROOT)
            FileChannel.open(lockFile(label), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use {
                    action()
                }
            }
        } catch (e: IOException) {
            throw IllegalStateException("Failed to lock testcontainer lifecycle for $label", e)
        }
    }

    private fun isSharedLifecycleEnabled(): Boolean =
        System.getProperty(SHARED_LIFECYCLE_ENABLED, "true").toBoolean()

    private fun leaseDir(label: String): Path = LEASE_ROOT.resolve(safeLabel(label))

    private fun leaseFile(label: String): Path = leaseDir(label).resolve("$PID-$PROCESS_MARK.lease")

    private fun lockFile(label: String): Path = LEASE_ROOT.resolve("${safeLabel(label)}.lock")

    private fun safeLabel(label: String): String = label.replace(Regex("[^A-Za-z0-9_.-]"), "_")

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
