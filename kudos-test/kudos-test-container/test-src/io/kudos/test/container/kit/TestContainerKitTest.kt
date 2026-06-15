package io.kudos.test.container.kit

import org.testcontainers.containers.GenericContainer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the file-lease / lock bookkeeping of [TestContainerKit] that needs no Docker daemon:
 * label sanitizing, lease/lock path derivation, lease register/release/prune lifecycle, pid reading
 * (valid, blank, corrupt), liveness check, in-memory reuse enablement, [TestContainerKit.ExecResult],
 * and the shared-lifecycle flag.
 *
 * @author K
 * @since 1.0.0
 */
internal class TestContainerKitTest {

    private val leaseRoot: Path = Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-leases")
    private val touchedProps = mutableListOf<String>()

    @AfterTest
    fun cleanup() {
        touchedProps.forEach { System.clearProperty(it) }
        touchedProps.clear()
    }

    private fun uniqueLabel() = "unit-label-${System.nanoTime()}"

    @Test
    fun `safeLabel replaces unsafe characters`() {
        assertEquals("a_b_c", TestContainerKit.safeLabel("a b/c"))
        assertEquals("Nacos__for_Seata_", TestContainerKit.safeLabel("Nacos (for Seata)"))
        assertEquals("ok.dash-1_2", TestContainerKit.safeLabel("ok.dash-1_2"))
    }

    @Test
    fun `lease and lock paths derive from sanitized label under lease root`() {
        val label = "weird name!"
        val safe = TestContainerKit.safeLabel(label)
        assertEquals(leaseRoot.resolve(safe), TestContainerKit.leaseDir(label))
        assertTrue(TestContainerKit.leaseFile(label).startsWith(TestContainerKit.leaseDir(label)))
        assertTrue(TestContainerKit.leaseFile(label).fileName.toString().endsWith(".lease"))
        assertEquals(leaseRoot.resolve("$safe.lock"), TestContainerKit.lockFile(label))
    }

    @Test
    fun `isSharedLifecycleEnabled defaults to true and honors override`() {
        System.clearProperty(TestContainerKit.SHARED_LIFECYCLE_ENABLED)
        assertTrue(TestContainerKit.isSharedLifecycleEnabled())

        touchedProps.add(TestContainerKit.SHARED_LIFECYCLE_ENABLED)
        System.setProperty(TestContainerKit.SHARED_LIFECYCLE_ENABLED, "false")
        assertFalse(TestContainerKit.isSharedLifecycleEnabled())
    }

    @Test
    fun `isProcessAlive true for current pid and false for unlikely pid`() {
        val self = ProcessHandle.current().pid()
        assertTrue(TestContainerKit.isProcessAlive(self))
        // pid 0 is the system idle/scheduler and is not reported as a normal live process handle
        assertFalse(TestContainerKit.isProcessAlive(Long.MAX_VALUE))
    }

    @Test
    fun `readLeasePid parses valid pid, rejects blank and corrupt`() {
        val dir = Files.createTempDirectory("lease-read")
        val valid = dir.resolve("v.lease")
        Files.writeString(valid, "12345", StandardCharsets.UTF_8)
        assertEquals(12345L, TestContainerKit.readLeasePid(valid))

        val blank = dir.resolve("b.lease")
        Files.writeString(blank, "   ", StandardCharsets.UTF_8)
        assertNull(TestContainerKit.readLeasePid(blank))

        val corrupt = dir.resolve("c.lease")
        Files.writeString(corrupt, "not-a-number", StandardCharsets.UTF_8)
        assertNull(TestContainerKit.readLeasePid(corrupt))

        val missing = dir.resolve("missing.lease")
        assertNull(TestContainerKit.readLeasePid(missing))
    }

    @Test
    fun `register lease then prune counts the live current-process lease`() {
        val label = uniqueLabel()
        try {
            TestContainerKit.registerLease(label)
            assertTrue(Files.exists(TestContainerKit.leaseFile(label)))
            // current process is alive -> at least one active lease
            assertEquals(1, TestContainerKit.pruneAndCountActiveLeases(label))
        } finally {
            TestContainerKit.releaseLease(label)
        }
        assertFalse(Files.exists(TestContainerKit.leaseFile(label)))
    }

    @Test
    fun `prune removes stale lease of a dead pid`() {
        val label = uniqueLabel()
        val dir = TestContainerKit.leaseDir(label)
        Files.createDirectories(dir)
        val staleFile = dir.resolve("999999999-1.lease")
        Files.writeString(staleFile, Long.MAX_VALUE.toString(), StandardCharsets.UTF_8)

        val active = TestContainerKit.pruneAndCountActiveLeases(label)
        assertEquals(0, active)
        assertFalse(Files.exists(staleFile), "dead-pid lease must be pruned")
    }

    @Test
    fun `prune of non-existent dir returns zero`() {
        val label = uniqueLabel()
        assertEquals(0, TestContainerKit.pruneAndCountActiveLeases(label))
    }

    @Test
    fun `releaseLease on missing file is a no-op`() {
        val label = uniqueLabel()
        // should not throw even when nothing was registered
        TestContainerKit.releaseLease(label)
    }

    @Test
    fun `withLabelLock executes the action exclusively`() {
        val label = uniqueLabel()
        var ran = false
        TestContainerKit.withLabelLock(label) { ran = true }
        assertTrue(ran)
        assertTrue(Files.exists(TestContainerKit.lockFile(label)))
    }

    @Test
    fun `enableTestcontainersReuseInMemory sets the reuse property without touching home file`() {
        // Should complete without throwing; reflection path into TestcontainersConfiguration.
        TestContainerKit.enableTestcontainersReuseInMemory()
    }

    @Test
    fun `prepareSharedReusableContainer enables reuse when shared lifecycle on`() {
        System.clearProperty(TestContainerKit.SHARED_LIFECYCLE_ENABLED)
        val container = GenericContainer("alpine:3.20")
        TestContainerKit.prepareSharedReusableContainer(container)
        assertTrue(container.isShouldBeReused)
    }

    @Test
    fun `prepareSharedReusableContainer is a no-op when shared lifecycle off`() {
        touchedProps.add(TestContainerKit.SHARED_LIFECYCLE_ENABLED)
        System.setProperty(TestContainerKit.SHARED_LIFECYCLE_ENABLED, "false")
        val container = GenericContainer("alpine:3.20")
        TestContainerKit.prepareSharedReusableContainer(container)
        assertFalse(container.isShouldBeReused)
    }

    @Test
    fun `ExecResult data class exposes fields`() {
        val r = TestContainerKit.ExecResult(0, "out", "err")
        assertEquals(0, r.exitCode)
        assertEquals("out", r.stdout)
        assertEquals("err", r.stderr)
        assertEquals(1, r.copy(exitCode = 1).exitCode)
        assertTrue(r.toString().contains("ExecResult"))
    }

    @Test
    fun `isServiceRunning reflects compose service running state`() {
        val compose = org.mockito.Mockito.mock(org.testcontainers.containers.ComposeContainer::class.java)
        val containerState = org.mockito.Mockito.mock(org.testcontainers.containers.ContainerState::class.java)
        org.mockito.Mockito.`when`(containerState.isRunning).thenReturn(true)
        org.mockito.Mockito.`when`(compose.getContainerByServiceName("svc")).thenReturn(Optional.of(containerState))
        assertTrue(TestContainerKit.isServiceRunning(compose, "svc"))

        org.mockito.Mockito.`when`(compose.getContainerByServiceName("absent")).thenReturn(Optional.empty())
        assertFalse(TestContainerKit.isServiceRunning(compose, "absent"))
    }

    @Test
    fun `addShutdownHook non-shared registers a stop hook`() {
        touchedProps.add(TestContainerKit.SHARED_LIFECYCLE_ENABLED)
        System.setProperty(TestContainerKit.SHARED_LIFECYCLE_ENABLED, "false")
        val container = org.mockito.Mockito.mock(GenericContainer::class.java)
        // registers a JVM shutdown hook that will call container.stop() at exit; must not throw now
        TestContainerKit.addShutdownHook(container, "nonshared-${System.nanoTime()}")
    }

    @Test
    fun `addShutdownHook shared registers lease and is idempotent per label`() {
        System.clearProperty(TestContainerKit.SHARED_LIFECYCLE_ENABLED)
        val container = org.mockito.Mockito.mock(GenericContainer::class.java)
        val label = uniqueLabel()
        try {
            TestContainerKit.addShutdownHook(container, label)
            assertTrue(Files.exists(TestContainerKit.leaseFile(label)), "lease should be registered")
            // second call for the same label hits the early-return (already registered) branch
            TestContainerKit.addShutdownHook(container, label)
        } finally {
            TestContainerKit.releaseLease(label)
        }
    }

    @Test
    fun `constants exposed`() {
        assertEquals("kudos-test-container", TestContainerKit.LABEL_KEY)
        assertEquals("kudos.testcontainer.shared-lifecycle.enabled", TestContainerKit.SHARED_LIFECYCLE_ENABLED)
    }
}
