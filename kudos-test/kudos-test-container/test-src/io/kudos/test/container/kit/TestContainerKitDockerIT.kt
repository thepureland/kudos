package io.kudos.test.container.kit

import io.kudos.test.container.FakeDynamicPropertyRegistry
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import io.kudos.test.container.main.ManualTestContainerMainSupport
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Docker-gated integration coverage for the daemon-touching paths of [TestContainerKit],
 * [DockerKit] and [ManualTestContainerMainSupport]. Runs only when Docker is installed; uses the
 * tiny `redis:*-alpine` image (fast pull/start) and cleans up after itself.
 *
 * Because [RedisTestContainer] holds a single reusable container instance per JVM, the whole
 * lifecycle is driven from one ordered test rather than stop/restart cycles that the reuse cache
 * would short-circuit.
 *
 * Covers: [DockerKit.ensureDockerRunning] / [DockerKit.isDockerRunning], [TestContainerKit.getDockerClient],
 * [TestContainerKit.getRunningContainer] (absent + present), [TestContainerKit.isContainerRunning],
 * [TestContainerKit.startContainerIfNeeded] (first-start + already-started branches),
 * [TestContainerKit.execInContainer], the "keep alive while a live lease remains" and the
 * "stop when last" branches of [TestContainerKit.releaseAndStopIfLast] /
 * [TestContainerKit.stopContainerByLabel], and [ManualTestContainerMainSupport.removeExistingContainers].
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
internal class TestContainerKitDockerIT {

    private val label = RedisTestContainer.LABEL

    @Test
    fun `daemon-only probes work without any container`() {
        DockerKit.ensureDockerRunning()
        assertTrue(DockerKit.isDockerRunning())
        assertNotNull(TestContainerKit.getDockerClient())

        val absentLabel = "no-such-label-${System.nanoTime()}"
        assertNull(TestContainerKit.getRunningContainer(absentLabel))
        assertFalse(TestContainerKit.isContainerRunning(absentLabel))
    }

    @Test
    fun `full redis lifecycle exercises start, reuse, register, exec, lease and stop`() {
        // clean slate
        ManualTestContainerMainSupport.removeExistingContainers(label, "Redis")
        try {
            // first start (first-start branch of startContainerIfNeeded)
            val reg = FakeDynamicPropertyRegistry()
            val running = RedisTestContainer.startIfNeeded(reg)
            assertNotNull(running)
            assertTrue(TestContainerKit.isContainerRunning(label))
            assertTrue(reg.has("spring.data.redis.host"))
            assertTrue(reg.has("spring.data.redis.port"))
            assertTrue(reg.has("kudos.ability.data.redis.redis-map.data.host"))

            // already-started branch (prints "has already been started", re-adds shutdown hook)
            val running2 = RedisTestContainer.startIfNeeded(null)
            assertEquals(running.id, running2.id)
            assertNotNull(RedisTestContainer.getRunningContainer())

            // exec a command inside the container via the docker-java exec path
            val current = TestContainerKit.getRunningContainer(label)
            assertNotNull(current)
            val execResult = TestContainerKit.execInContainer(current, "redis-cli", "PING")
            assertEquals(0, execResult.exitCode)
            assertTrue(execResult.stdout.contains("PONG", ignoreCase = true))

            // "keep alive while a live lease remains": plant a live-pid lease, then release.
            TestContainerKit.withLabelLock(label) {
                val dir = TestContainerKit.leaseDir(label)
                Files.createDirectories(dir)
                val self = ProcessHandle.current().pid()
                Files.writeString(dir.resolve("$self-extra.lease"), self.toString())
            }
            TestContainerKit.releaseAndStopIfLast(label)
            assertTrue(TestContainerKit.isContainerRunning(label), "live lease should keep container up")

            // remove the planted lease, then "stop when last" path actually stops the container
            TestContainerKit.withLabelLock(label) {
                val dir = TestContainerKit.leaseDir(label)
                Files.list(dir).use { s -> s.forEach { Files.deleteIfExists(it) } }
            }
            TestContainerKit.releaseAndStopIfLast(label)
            // assert the specific started container id is gone (robust against a parallel build that
            // may legitimately be running its own Redis under the same shared label)
            val stillThere = TestContainerKit.getRunningContainer(label)
            assertTrue(stillThere == null || stillThere.id != running.id, "our container should be stopped")
        } finally {
            runCatching { ManualTestContainerMainSupport.removeExistingContainers(label, "Redis") }
        }
    }

    @Test
    fun `manual support removeExistingContainers is a no-op when nothing matches`() {
        val absent = "manual-absent-${System.nanoTime()}"
        // empty-list branch: must not throw
        ManualTestContainerMainSupport.removeExistingContainers(absent, "Nothing")
    }

    @Test
    fun `manual support removes a running labelled container`() {
        // A standalone container under a unique label, so this never collides with reuse of the
        // RedisTestContainer singleton, and exercises ManualTestContainerMainSupport.removeContainer.
        val uniqueLabel = "ManualIT-${System.nanoTime()}"
        val container = org.testcontainers.containers.GenericContainer("redis:8.6.0-alpine")
            .withExposedPorts(6379)
            .withLabel(TestContainerKit.LABEL_KEY, uniqueLabel)
        TestContainerKit.startContainer(container, uniqueLabel)
        try {
            assertNotNull(TestContainerKit.getRunningContainer(uniqueLabel))
            // exercise the GenericContainer exec overload against the live container
            val exec = TestContainerKit.execInContainer(container, "redis-cli", "PING")
            assertEquals(0, exec.exitCode)
            assertTrue(exec.stdout.contains("PONG", ignoreCase = true))
            // remove-while-running covers the removeContainer loop (force + volumes)
            ManualTestContainerMainSupport.removeExistingContainers(uniqueLabel, "Redis-manual")
            assertNull(TestContainerKit.getRunningContainer(uniqueLabel))
        } finally {
            runCatching { ManualTestContainerMainSupport.removeExistingContainers(uniqueLabel, "Redis-manual") }
            runCatching { container.stop() }
        }
    }
}
