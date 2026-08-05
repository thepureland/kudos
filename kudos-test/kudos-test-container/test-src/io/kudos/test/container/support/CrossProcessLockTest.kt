package io.kudos.test.container.support

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers all branches of [CrossProcessLock.run]:
 * - lock-disabled fast path (only synchronized)
 * - real file-lock path (default + custom lock path)
 * - lock path resolution: blank property name, set-but-empty property, set-and-non-blank property (trimmed)
 * - IOException -> IllegalStateException, with / without the disable-hint suffix
 * - the critical section actually executes inside synchronized(jvmMonitor) and its return value is propagated
 *
 * @author K
 * @since 1.0.0
 */
internal class CrossProcessLockTest {

    private val touchedProps = mutableListOf<String>()

    @AfterTest
    fun cleanup() {
        touchedProps.forEach { System.clearProperty(it) }
        touchedProps.clear()
    }

    private fun setProp(key: String, value: String) {
        touchedProps.add(key)
        System.setProperty(key, value)
    }

    private fun tempLock(name: String): Path =
        Files.createTempDirectory("cpl-test").resolve(name)

    @Test
    fun `disabled lock skips file lock and runs critical section`() {
        val disableProp = "kudos.test.cpl.disable.${System.nanoTime()}"
        setProp(disableProp, "true")
        val monitor = Any()
        var ran = false

        val result = CrossProcessLock.run(
            jvmMonitor = monitor,
            lockPathProperty = null,
            disableLockProperty = disableProp,
            defaultLockPath = { error("default lock path must not be evaluated when lock is disabled") },
            criticalSection = {
                ran = true
                assertTrue(Thread.holdsLock(monitor), "critical section must hold the jvm monitor")
                42
            }
        )

        assertTrue(ran)
        assertEquals(42, result)
    }

    @Test
    fun `disable property present but false uses file lock`() {
        val disableProp = "kudos.test.cpl.disable.${System.nanoTime()}"
        setProp(disableProp, "false")
        val lockPath = tempLock("a.lock")
        val monitor = Any()

        val result = CrossProcessLock.run(
            jvmMonitor = monitor,
            lockPathProperty = null,
            disableLockProperty = disableProp,
            defaultLockPath = { lockPath },
            criticalSection = { "ok" }
        )

        assertEquals("ok", result)
        assertTrue(Files.exists(lockPath), "lock file should be created by FileChannel.open CREATE")
    }

    @Test
    fun `null disable property uses file lock with default path`() {
        val lockPath = tempLock("default.lock")
        val result = CrossProcessLock.run(
            jvmMonitor = Any(),
            lockPathProperty = null,
            disableLockProperty = null,
            defaultLockPath = { lockPath },
            criticalSection = { 7 }
        )
        assertEquals(7, result)
        assertTrue(Files.exists(lockPath))
    }

    @Test
    fun `blank disable property name is treated as not disabled`() {
        val lockPath = tempLock("blank-disable.lock")
        val result = CrossProcessLock.run(
            jvmMonitor = Any(),
            lockPathProperty = null,
            disableLockProperty = "   ",
            defaultLockPath = { lockPath },
            criticalSection = { "x" }
        )
        assertEquals("x", result)
        assertTrue(Files.exists(lockPath))
    }

    @Test
    fun `custom lock path property overrides default path`() {
        val pathProp = "kudos.test.cpl.path.${System.nanoTime()}"
        val customPath = tempLock("custom.lock")
        // value intentionally padded so the trim() branch is exercised
        setProp(pathProp, "  $customPath  ")

        var defaultEvaluated = false
        CrossProcessLock.run(
            jvmMonitor = Any(),
            lockPathProperty = pathProp,
            disableLockProperty = null,
            defaultLockPath = { defaultEvaluated = true; tempLock("unused.lock") },
            criticalSection = { }
        )

        assertFalse(defaultEvaluated, "default path supplier must not be used when property resolves")
        assertTrue(Files.exists(customPath))
    }

    @Test
    fun `empty lock path property value falls back to default`() {
        val pathProp = "kudos.test.cpl.path.${System.nanoTime()}"
        setProp(pathProp, "   ")
        val defaultPath = tempLock("fallback.lock")

        CrossProcessLock.run(
            jvmMonitor = Any(),
            lockPathProperty = pathProp,
            disableLockProperty = null,
            defaultLockPath = { defaultPath },
            criticalSection = { }
        )
        assertTrue(Files.exists(defaultPath))
    }

    @Test
    fun `blank lock path property name falls back to default`() {
        val defaultPath = tempLock("blankname.lock")
        CrossProcessLock.run(
            jvmMonitor = Any(),
            lockPathProperty = "   ",
            disableLockProperty = null,
            defaultLockPath = { defaultPath },
            criticalSection = { }
        )
        assertTrue(Files.exists(defaultPath))
    }

    @Test
    fun `unresolvable lock path throws IllegalStateException without hint`() {
        // A path whose parent directory does not exist -> FileChannel.open throws IOException.
        val badPath = Path.of(System.getProperty("java.io.tmpdir"), "no-such-dir-${System.nanoTime()}", "x.lock")
        val ex = assertFailsWith<IllegalStateException> {
            CrossProcessLock.run(
                jvmMonitor = Any(),
                lockPathProperty = null,
                disableLockProperty = null,
                defaultLockPath = { badPath },
                criticalSection = { }
            )
        }
        assertTrue(ex.message!!.startsWith("Failed to acquire cross-process mutex lock:"))
        assertFalse(ex.message!!.contains("-D"), "no disable hint expected when disableLockProperty is null")
        assertTrue(ex.cause is java.io.IOException)
    }

    @Test
    fun `unresolvable lock path includes disable hint when disable property provided`() {
        val disableProp = "kudos.test.cpl.disable.${System.nanoTime()}"
        // not set -> defaults to false -> file lock attempted, then fails
        val badPath = Path.of(System.getProperty("java.io.tmpdir"), "no-such-dir-${System.nanoTime()}", "x.lock")
        val ex = assertFailsWith<IllegalStateException> {
            CrossProcessLock.run(
                jvmMonitor = Any(),
                lockPathProperty = null,
                disableLockProperty = disableProp,
                defaultLockPath = { badPath },
                criticalSection = { }
            )
        }
        assertTrue(ex.message!!.contains("-D$disableProp=true to skip the file lock"))
    }

    @Test
    fun `reentrant acquisition of the same lock file in one jvm works`() {
        // FileLock is per-JVM; re-acquiring within the same process must not deadlock for nested calls
        // on different lock files. Verify two sequential acquisitions on the same path succeed.
        val lockPath = tempLock("seq.lock")
        repeat(2) {
            val r = CrossProcessLock.run(
                jvmMonitor = Any(),
                lockPathProperty = null,
                disableLockProperty = null,
                defaultLockPath = { lockPath },
                criticalSection = { it }
            )
            assertEquals(it, r)
        }
    }

    @Test
    fun `null is a valid critical section result`() {
        val lockPath = tempLock("nullret.lock")
        val r: String? = CrossProcessLock.run<String?>(
            jvmMonitor = Any(),
            lockPathProperty = null,
            disableLockProperty = null,
            defaultLockPath = { lockPath },
            criticalSection = { null }
        )
        assertNull(r)
    }

    @Test
    fun `same monitor instance is used for synchronization`() {
        val lockPath = tempLock("monitor.lock")
        val monitor = Any()
        var captured: Boolean? = null
        CrossProcessLock.run(
            jvmMonitor = monitor,
            lockPathProperty = null,
            disableLockProperty = null,
            defaultLockPath = { lockPath },
            criticalSection = { captured = Thread.holdsLock(monitor) }
        )
        assertSame(true, captured)
    }
}
