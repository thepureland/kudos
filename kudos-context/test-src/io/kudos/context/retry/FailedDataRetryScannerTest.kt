package io.kudos.context.retry

import io.kudos.context.lock.ILeaseLockProvider
import io.kudos.context.lock.NormalLockService
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for FailedDataRetryScanner.
 *
 * Coverage:
 * - [FailedDataRetryScanner.retry] file scanning: success deletes / failure retains / exception retains
 * - Empty directories are automatically cleaned up after scanning
 * - [FailedDataRetryScanner.lockRetry] lock interactions: retry runs only after the lock is acquired; finally always releases
 *
 * Test strategy:
 * - Internal methods are visible via `internal`; called directly
 * - File operations use [createTempDirectory]; each case self-cleans
 * - Lock injection via [FailedDataRetryScanner.lockProviderSupplier] bypasses [LockTool]'s Spring dependency
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class FailedDataRetryScannerTest {

    private lateinit var tempRoot: Path
    private lateinit var scanner: FailedDataRetryScanner

    @BeforeTest
    fun setup() {
        KudosContextHolder.clear()
        tempRoot = createTempDirectory("kudos-fdrs-test-")
        scanner = FailedDataRetryScanner()
    }

    @AfterTest
    fun cleanup() {
        KudosContextHolder.clear()
        // Recursively delete the entire temp directory
        tempRoot.toFile().walkBottomUp().forEach { it.delete() }
    }

    /** Custom handler that points filePath to a subdirectory under tempRoot. */
    private inner class StubHandler(
        override val businessType: String,
        private val processFn: (StubData) -> Boolean
    ) : IFailedDataHandler<StubData> {

        override val cronExpression: String = "0 0/1 * * * *"

        var processInvocations = 0
            private set

        override fun persistFailedData(data: StubData): String = "ignored"

        override fun handleFailedData(file: File): Boolean {
            processInvocations++
            // Simply use the file name as a marker; processFn decides the outcome
            val data = StubData(file.name)
            return processFn(data)
        }

        override fun filePath(): String = tempRoot.toString()
    }

    private data class StubData(val marker: String)

    /** Places a properly named failed-data file inside the handler's business directory. */
    private fun seedFile(businessType: String, content: String = "{}"): Path {
        val businessDir = tempRoot.resolve(businessType)
        Files.createDirectories(businessDir)
        val name = "${System.currentTimeMillis()}-${UUID.randomUUID()}.json"
        val file = businessDir.resolve(name)
        Files.write(file, content.toByteArray())
        return file
    }

    // ============================================================
    // retry — file scanning and deletion
    // ============================================================

    @Test
    fun retryDeletesFileWhenHandlerReturnsTrue() {
        val handler = StubHandler("bizA") { true }
        val file1 = seedFile("bizA")
        val file2 = seedFile("bizA")

        scanner.retry(handler)

        assertEquals(2, handler.processInvocations, "each of the two files should be processed once")
        assertFalse(Files.exists(file1), "successfully processed files should be deleted")
        assertFalse(Files.exists(file2))
        // The business directory should be cleaned up once it contains no files
        assertFalse(Files.exists(tempRoot.resolve("bizA")), "empty directory should be cleaned up")
    }

    @Test
    fun retryKeepsFileWhenHandlerReturnsFalse() {
        val handler = StubHandler("bizB") { false }
        val file = seedFile("bizB")

        scanner.retry(handler)

        assertEquals(1, handler.processInvocations)
        assertTrue(Files.exists(file), "failed files should be retained for the next retry")
        // The directory still has files and should not be deleted
        assertTrue(Files.exists(tempRoot.resolve("bizB")))
    }

    @Test
    fun retrySwallowsHandlerExceptionAndKeepsFile() {
        val handler = StubHandler("bizC") { throw IllegalStateException("processing blew up") }
        val file = seedFile("bizC")

        // Exceptions should not propagate up — the scanner catches and logs them internally
        scanner.retry(handler)

        assertEquals(1, handler.processInvocations)
        assertTrue(Files.exists(file), "files should be retained on the exception path")
    }

    @Test
    fun retryHandlesMixedSuccessAndFailure() {
        // The handler returns true on even invocations and false on odd ones
        val counter = AtomicInteger(0)
        val handler = StubHandler("bizD") { counter.incrementAndGet() % 2 == 0 }
        val files = (1..4).map { seedFile("bizD") }

        scanner.retry(handler)

        // Files are processed in sorted file-name order: invocations 1 and 3 return false (retained), 2 and 4 return true (deleted)
        // (Exactly which files are deleted depends on sorted() order, but the totals should be 2 successes and 2 failures.)
        val remaining = files.count { Files.exists(it) }
        assertEquals(2, remaining, "two files should be retained (the failed ones)")
    }

    @Test
    fun retryIgnoresUnrelatedFiles() {
        // Files not matching the `{millis}-{uuid}.json` naming pattern should be filtered out
        val businessDir = tempRoot.resolve("bizE")
        Files.createDirectories(businessDir)
        val unrelated = businessDir.resolve("notes.txt")
        Files.write(unrelated, "ignore me".toByteArray())

        val handler = StubHandler("bizE") { true }
        scanner.retry(handler)

        assertEquals(0, handler.processInvocations, "should not process unrelated files")
        assertTrue(Files.exists(unrelated), "unrelated files should not be deleted")
    }

    @Test
    fun retryReturnsImmediatelyWhenDirectoryDoesNotExist() {
        val handler = StubHandler("bizMissing") { true }
        // Do not create the bizMissing directory
        scanner.retry(handler)
        assertEquals(0, handler.processInvocations, "no directory means nothing to do")
    }

    // ============================================================
    // lockRetry — lock interaction
    // ============================================================

    @Test
    fun currentAtomicServiceCodeDoesNotCreateContextWhenMissing() {
        assertEquals(null, scanner.currentAtomicServiceCode())
        assertEquals(null, KudosContextHolder.getOrNull(), "reading the scheduling-lock dimension should not implicitly create a KudosContext")
    }

    @Test
    fun currentAtomicServiceCodeReadsExistingContext() {
        KudosContextHolder.set(KudosContext().apply { atomicServiceCode = "atomic-a" })

        assertEquals("atomic-a", scanner.currentAtomicServiceCode())
    }

    @Test
    fun lockRetryRunsRetryWhenLockAcquired() {
        val handler = StubHandler("bizLock") { true }
        seedFile("bizLock")

        // Inject a fresh local lock service so tryLock always succeeds
        scanner.lockProviderSupplier = { NormalLockService() }

        scanner.lockRetry(handler, appName = "test-svc")

        assertEquals(1, handler.processInvocations, "retry should run after the lock is acquired")
    }

    @Test
    fun lockRetrySkipsRetryWhenLockBusy() {
        val handler = StubHandler("bizBusy") { true }
        seedFile("bizBusy")

        // A stub lock that always fails to acquire
        scanner.lockProviderSupplier = {
            object : ILeaseLockProvider {
                override fun tryLock(lockKey: String, sec: Int): Boolean = false
                override fun unLock(key: String) {
                    error("unLock should not be called in the lockBusy test because tryLock failed")
                }
            }
        }

        scanner.lockRetry(handler, appName = "test-svc")

        assertEquals(0, handler.processInvocations, "retry should not run when the lock cannot be acquired")
    }

    @Test
    fun lockRetryReleasesLockEvenIfRetryThrows() {
        // When the handler throws inside retry, the scanner catches it internally and does not propagate, so lockRetry does not exit exceptionally.
        // This test guarantees: unLock is always invoked (the finally path takes effect).
        val unlockCalled = AtomicBoolean(false)
        val handler = StubHandler("bizThrow") { throw RuntimeException("boom") }
        seedFile("bizThrow")

        scanner.lockProviderSupplier = {
            object : ILeaseLockProvider {
                override fun tryLock(lockKey: String, sec: Int): Boolean = true
                override fun unLock(key: String) {
                    unlockCalled.set(true)
                }
            }
        }

        scanner.lockRetry(handler, appName = "test-svc")

        assertTrue(unlockCalled.get(), "the lock must be released even when the handler throws (finally)")
    }
}
