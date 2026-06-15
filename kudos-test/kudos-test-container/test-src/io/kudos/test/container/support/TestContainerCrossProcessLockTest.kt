package io.kudos.test.container.support

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [TestContainerCrossProcessLock.run]:
 * - default lock path derivation (`kudos-testcontainer-<id>.lock` under java.io.tmpdir)
 * - custom lock-file system property override (`kudos.testcontainer.<id>.lock.file`)
 * - disable system property short-circuit (`kudos.testcontainer.<id>.lock.disable`)
 * - critical section return value propagation and the monitor being the supplied class
 *
 * @author K
 * @since 1.0.0
 */
internal class TestContainerCrossProcessLockTest {

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

    @Test
    fun `runs critical section with default lock path under tmpdir`() {
        val id = "unit-${System.nanoTime()}"
        val result = TestContainerCrossProcessLock.run(TestContainerCrossProcessLockTest::class.java, id) { "done" }
        assertEquals("done", result)

        val expected = Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-$id.lock")
        assertTrue(Files.exists(expected), "default lock file should be created at $expected")
        Files.deleteIfExists(expected)
    }

    @Test
    fun `honors custom lock file system property`() {
        val id = "unit-${System.nanoTime()}"
        val custom = Files.createTempDirectory("tccpl").resolve("custom-$id.lock")
        setProp("kudos.testcontainer.$id.lock.file", custom.toString())

        TestContainerCrossProcessLock.run(TestContainerCrossProcessLockTest::class.java, id) { }

        assertTrue(Files.exists(custom))
        val defaultPath = Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-$id.lock")
        assertTrue(!Files.exists(defaultPath), "default lock file must not be created when custom path is set")
    }

    @Test
    fun `disable property skips file lock entirely`() {
        val id = "unit-${System.nanoTime()}"
        setProp("kudos.testcontainer.$id.lock.disable", "true")

        val result = TestContainerCrossProcessLock.run(TestContainerCrossProcessLockTest::class.java, id) { 99 }

        assertEquals(99, result)
        val defaultPath = Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-$id.lock")
        assertTrue(!Files.exists(defaultPath), "no lock file should be created when disabled")
    }
}
