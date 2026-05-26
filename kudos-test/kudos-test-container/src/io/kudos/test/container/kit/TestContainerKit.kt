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
 * test-container utility.
 *
 * When [SHARED_LIFECYCLE_ENABLED] is enabled (default), multiple JVMs share the same docker container via local file leases:
 * - The container is started only when no running container with the same label exists
 * - Each JVM that uses the container registers its own lease file
 * - JVM shutdown hooks only release the current process's lease; the container is actually stopped only when the last live lease leaves
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object TestContainerKit {

    const val LABEL_KEY = "kudos-test-container"

    /**
     * Whether to enable cross-JVM shared lifecycle. Enabled by default; set to `false` to revert to the old behavior.
     */
    const val SHARED_LIFECYCLE_ENABLED = "kudos.testcontainer.shared-lifecycle.enabled"

    val DEFAULT_DOCKER_NETWORK: Network = Network.newNetwork()

    private val LEASE_ROOT: Path =
        Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-leases")
    private val PID: Long = ProcessHandle.current().pid()
    private val PROCESS_MARK: Long = System.nanoTime()
    private val REGISTERED_LABELS: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Starts the container (if needed).
     *
     * Ensures a single container is shared across a batch of tests, avoiding the time wasted starting/stopping containers repeatedly.
     * Alternatively, you can run a class's main method manually to start the container and share it while running tests.
     * Registers a JVM shutdown hook to automatically stop the container when the batch finishes,
     * rather than stopping after each test — provided the @Testcontainers annotation is not used.
     *
     * When multiple JVMs connect to the same container, users are tracked via local leases; a JVM that exits first will not stop a container still in use by other JVMs,
     * and the container is stopped only after the last live lease is released.
     * To skip tests when Docker is not installed, use @EnabledIfDockerInstalled.
     *
     * @param label the value of the container's label (the key must be "label")
     * @param container the container
     * @return the container instance
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
     * Whether the container is running.
     *
     * Note: container.isRunning only checks the current JVM's state value, while this method can detect whether it is running in Docker.
     *
     * @param label the value of the container's label (the key must be "label")
     * @return true if running, false otherwise
     */
    fun isContainerRunning(label: String): Boolean = getRunningContainer(label) != null

    /**
     * Whether the service is running.
     *
     * @param compose a container instance launched from compose.yml
     * @param serviceInstanceName the service instance name
     * @return true if running, false otherwise
     */
    fun isServiceRunning(compose: ComposeContainer, serviceInstanceName: String): Boolean =
        compose.getContainerByServiceName(serviceInstanceName)
            .map { it.isRunning }
            .orElse(false)

    /**
     * Gets the running container corresponding to the given label.
     *
     * Only returns truly running containers (`withShowAll(false)`); exited residual containers are not returned as "ready",
     * so subsequent on-demand startup is correctly triggered.
     *
     * @param label the value of the container's label (the key must be "label")
     * @return the container instance, or null if none exists
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
     * Starts the container.
     *
     * @param container the container
     * @param label the container's mnemonic name
     */
    fun startContainer(container: GenericContainer<*>, label: String) {
        println(">>>>>>>>>>>>>>>>>>>> Starting $label container...")
        prepareSharedReusableContainer(container)
        val time = measureTimeMillis { container.start() }
        println("<<<<<<<<<<<<<<<<<<<< $label container started in $time ms.")

        addShutdownHook(container, label)
    }

    /**
     * Registers a JVM shutdown hook to automatically stop the container.
     *
     * Lets the container be stopped only when the test batch ends, rather than after each test — provided the @Testcontainers annotation is not used.
     *
     * When shared lifecycle is enabled, the shutdown hook only releases the current JVM's lease; if other live JVM leases remain, the container is retained.
     *
     * @param container the container
     * @param label the container's mnemonic name
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
     * Shared containers must avoid Testcontainers/Ryuk's single-JVM session cleanup;
     * otherwise the container would be reaped by Ryuk when the first starter leaves.
     */
    private fun prepareSharedReusableContainer(container: GenericContainer<*>) {
        if (!isSharedLifecycleEnabled()) return
        enableTestcontainersReuseInMemory()
        container.withReuse(true)
    }

    /**
     * Enables reuse only in the current JVM's memory; does not write to the user's home `~/.testcontainers.properties`.
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
     * Releases the current JVM's lease, and stops the container for the given label once no other live leases remain.
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
     * Finds and stops the container directly by Docker label, avoiding any dependency on whether the current JVM still holds the original [GenericContainer] instance.
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
     * The lease file name includes the pid and an intra-process unique value, preventing accidental deletion of a new process's lease when the OS reuses the same pid.
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
     * Deletes the current JVM's lease file for the given container label.
     */
    private fun releaseLease(label: String) {
        try {
            Files.deleteIfExists(leaseFile(label))
        } catch (e: IOException) {
            System.err.println("Failed to release testcontainer lease for $label: ${e.message}")
        }
    }

    /**
     * Removes stale lease files and returns the count of still-live JVM leases.
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
     * Reads the pid from a lease file; treats corrupted files or invalid contents as stale leases.
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
     * Returns whether a process with the given pid is still alive.
     */
    private fun isProcessAlive(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    /**
     * Lease add/remove and the last-stop decision for the same label must be mutually exclusive, preventing two JVMs from concurrently concluding they are the last user.
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
     * Executes a command directly inside the container.
     *
     * @param container the container instance
     * @param command the command and its arguments to execute
     * @return the execution result, containing exit code, stdout, and stderr
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
     * Executes a command inside the container via the Docker Java API.
     *
     * @param container the Container object from the Docker Java API
     * @param command the command and its arguments to execute
     * @return the execution result, containing exit code, stdout, and stderr
     */
    fun execInContainer(container: Container, vararg command: String): ExecResult {
        val dockerClient = getDockerClient()
        val containerId = container.id

        // Create the exec command
        val execCreateCmd = dockerClient.execCreateCmd(containerId)
            .withCmd(*command)
            .withAttachStdout(true)
            .withAttachStderr(true)

        val execCreateResponse: ExecCreateCmdResponse = execCreateCmd.exec()
        val execId = execCreateResponse.id

        // Execute the command and capture the output
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                try {
                    when (frame.streamType) {
                        StreamType.STDOUT -> stdout.write(frame.payload)
                        StreamType.STDERR -> stderr.write(frame.payload)
                        else -> {
                            // Ignore other types (e.g. RAW), or handle as appropriate
                        }
                    }
                } catch (e: IOException) {
                    throw RuntimeException("Failed to write frame output", e)
                }
            }
        }

        dockerClient.execStartCmd(execId).exec(callback).awaitCompletion()

        // Get the exit code
        val inspectExecResponse = dockerClient.inspectExecCmd(execId).exec()
        val exitCode = inspectExecResponse.exitCodeLong ?: -1

        return ExecResult(
            exitCode = exitCode.toInt(),
            stdout = stdout.toString(),
            stderr = stderr.toString()
        )
    }

    /**
     * Execution result data class.
     */
    data class ExecResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    /**
     * Obtains a DockerClient instance (using Testcontainers' DockerClientFactory).
     */
    fun getDockerClient(): DockerClient {
        return DockerClientFactory.instance().client()
    }


}
