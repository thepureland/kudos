package io.kudos.context.retry

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.NormalLockService
import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticApplicationContext
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.support.CronTrigger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for FailedDataRetryScanner.scheduleAll
 *
 * Coverage:
 * - One scheduled task is registered per IFailedDataHandler bean found in the Spring context
 * - The task fires with the handler's own cron expression (CronTrigger)
 * - Executing the captured task body runs the full lockRetry path end-to-end
 *   (lock acquired -> file processed -> file deleted)
 *
 * Strategy: SpringKit gets a StaticApplicationContext holding the stub handler; the private
 * @Autowired taskScheduler field is filled with a recording fake via reflection (the field has no
 * setter by design); the previous global context is restored afterwards.
 *
 * @author K
 * @since 1.0.0
 */
internal class FailedDataRetryScannerScheduleAllTest {

    private class RecordingScheduler : TaskScheduler {
        val tasks = mutableListOf<Pair<Runnable, Trigger>>()
        override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*>? {
            tasks += task to trigger
            return null
        }

        override fun schedule(task: Runnable, startTime: Instant): ScheduledFuture<*> = unsupported()
        override fun scheduleAtFixedRate(task: Runnable, startTime: Instant, period: Duration): ScheduledFuture<*> =
            unsupported()

        override fun scheduleAtFixedRate(task: Runnable, period: Duration): ScheduledFuture<*> = unsupported()
        override fun scheduleWithFixedDelay(task: Runnable, startTime: Instant, delay: Duration): ScheduledFuture<*> =
            unsupported()

        override fun scheduleWithFixedDelay(task: Runnable, delay: Duration): ScheduledFuture<*> = unsupported()
        private fun unsupported(): Nothing = throw UnsupportedOperationException("not used by scheduleAll")
    }

    private class TempDirHandler(private val root: Path) : IFailedDataHandler<String> {
        override val businessType = "schedule-biz"
        override val cronExpression = "0 0 3 * * *"
        var handled = 0
            private set

        override fun persistFailedData(data: String): String? = null
        override fun handleFailedData(file: File): Boolean {
            handled++
            return true
        }

        override fun filePath(): String = root.toString()
    }

    private lateinit var tempRoot: Path
    private var previousContext: ApplicationContext? = null
    private lateinit var ownContext: StaticApplicationContext

    @BeforeTest
    fun setup() {
        KudosContextHolder.clear()
        tempRoot = createTempDirectory("kudos-fdrs-schedule-")
        previousContext = runCatching { SpringKit.applicationContext }.getOrNull()
        ownContext = StaticApplicationContext().also { it.refresh() }
        SpringKit.applicationContext = ownContext
    }

    @AfterTest
    fun cleanup() {
        SpringKit.applicationContext = previousContext
        ownContext.close()
        KudosContextHolder.clear()
        tempRoot.toFile().walkBottomUp().forEach { it.delete() }
    }

    private fun injectScheduler(scanner: FailedDataRetryScanner, scheduler: TaskScheduler) {
        FailedDataRetryScanner::class.java.getDeclaredField("taskScheduler").apply {
            isAccessible = true
            set(scanner, scheduler)
        }
    }

    @Test
    fun scheduleAllRegistersOneCronTaskPerHandlerAndTheTaskBodyWorks() {
        val handler = TempDirHandler(tempRoot)
        ownContext.beanFactory.registerSingleton("tempDirHandler", handler)

        // The scheduling thread carries an atomic service code -> it must flow into the lock key dimension
        KudosContextHolder.set(KudosContext().apply { atomicServiceCode = "app-x" })

        val scheduler = RecordingScheduler()
        val scanner = FailedDataRetryScanner()
        injectScheduler(scanner, scheduler)
        scanner.lockProviderSupplier = { NormalLockService() }

        scanner.scheduleAll()

        assertEquals(1, scheduler.tasks.size, "exactly one task per handler bean")
        val (task, trigger) = scheduler.tasks.single()
        assertTrue(trigger is CronTrigger)
        assertEquals("0 0 3 * * *", trigger.expression, "the handler's own cron expression must be used")

        // Fire the captured task body: full lockRetry -> retry -> delete flow
        val businessDir = tempRoot.resolve(handler.businessType)
        Files.createDirectories(businessDir)
        val dataFile = businessDir.resolve("${System.currentTimeMillis()}-${UUID.randomUUID()}.json")
        Files.write(dataFile, "{}".toByteArray())

        task.run()

        assertEquals(1, handler.handled, "the fired task must hand the file to the handler")
        assertFalse(Files.exists(dataFile), "a successfully handled file must be deleted")
    }

    @Test
    fun scheduleAllRegistersNothingWhenNoHandlersExist() {
        val scheduler = RecordingScheduler()
        val scanner = FailedDataRetryScanner()
        injectScheduler(scanner, scheduler)

        scanner.scheduleAll()

        assertEquals(0, scheduler.tasks.size)
    }
}
