package io.kudos.context.retry

import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.ILeaseLockProvider
import io.kudos.context.lock.LockTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
/**
 * Failed-data retry scanner.
 *
 * After application startup, creates a dedicated CRON scheduled task for each [IFailedDataHandler].
 * When the task fires, it first acquires a distributed lock (so only one node runs in multi-instance deployments),
 * then scans the handler's corresponding directory for failed-data files and hands each one back to the handler for retry; on success the file is deleted.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
class FailedDataRetryScanner {

    /** [TaskScheduler] dedicated to failed-data retry; the config class provides a separate thread pool to avoid consuming the business scheduler pool. */
    @Autowired
    @Qualifier("failDataTaskScheduler")
    private lateinit var taskScheduler: TaskScheduler

    /**
     * Lock provider resolution callback. Defaults to [LockTool.lockProvider] (requires the Spring context to be ready).
     *
     * Visible-for-testing: in unit tests, set this to e.g. `{ NormalLockService() }` (a local instance),
     * so [retry] / [lockRetry] can be tested without starting the Spring container.
     */
    internal var lockProviderSupplier: () -> ILeaseLockProvider = { LockTool.lockProvider }


    /**
     * Schedules all failed-data retry tasks.
     *
     * After application startup, creates a scheduled retry task for every IFailedDataHandler implementation.
     *
     * Flow:
     * 1. Obtain beans for all IFailedDataHandler implementations
     * 2. For each handler, create a scheduled task:
     *    - Use the handler's configured cronExpression as the firing schedule
     *    - When the task fires, call lockRetry (protected by a distributed lock)
     * 3. Log the scheduling information
     *
     * Scheduled tasks:
     * - Each handler has its own dedicated scheduled task
     * - Uses CronTrigger for flexible schedule configuration
     * - Acquires a distributed lock at task execution time so only one instance runs in multi-instance environments
     *
     * Notes:
     * - Runs in the @PostConstruct phase to ensure all beans are initialized
     * - Each handler's retry frequency can be configured independently
     * - Uses a distributed lock to avoid duplicate execution across instances
     */
    @jakarta.annotation.PostConstruct
    fun scheduleAll() {
        val handlers = SpringKit.getBeansOfType<IFailedDataHandler<*>>()
        handlers.values.forEach { handler ->
            taskScheduler.schedule(
                { lockRetry(handler, currentAtomicServiceCode()) },
                CronTrigger(handler.cronExpression)
            )
            logger.info("Scheduled retry for ${handler.businessType} [${handler.cronExpression}]")
        }
    }

    /**
     * Returns the atomic service code from the context bound to the current thread.
     *
     * Scheduler threads during startup usually have no request context, so [KudosContextHolder.getOrNull] must be used here
     * to avoid implicitly creating an empty ThreadLocal context just to generate a lock key.
     */
    internal fun currentAtomicServiceCode(): String? =
        KudosContextHolder.getOrNull()?.atomicServiceCode

    /**
     * Locked retry: protects the retry operation with a distributed lock.
     *
     * In a distributed environment, ensures only one instance executes the retry to avoid duplicate processing.
     *
     * Lock key format:
     * - `"$FAILED_DATA_RETRY_LOCK_PREFIX{businessType}_{appName}"`
     * - A historical version had a typo `faile-data-retry-`, now standardized to `failed-data-retry-`; multi-instance deployments must use the same prefix to remain mutually exclusive.
     *
     * @param handler the failed-data handler
     * @param appName the application name, used to build the lock key
     */
    internal fun lockRetry(handler: IFailedDataHandler<*>, appName: String?) {
        val lockProvider = lockProviderSupplier()
        val key = "$FAILED_DATA_RETRY_LOCK_PREFIX${handler.businessType}_$appName"
        val lock = lockProvider.tryLock(key, 600)
        if (!lock) {
            logger.warn("another task is still in progress; the lock has not been released")
            return
        }
        try {
            retry(handler)
        } finally {
            lockProvider.unLock(key)
        }
    }

    /**
     * Executes the retry: scans for failed-data files and processes them.
     *
     * @param handler the failed-data handler responsible for processing the specific failed data
     */
    internal fun retry(handler: IFailedDataHandler<*>) {
        val dir = Paths.get(handler.filePath(), handler.businessType)
        if (!Files.exists(dir)) {
            return
        }
        try {
            Files.list(dir).use { stream ->
                stream
                    .filter { p: Path ->
                        val name = p.fileName.toString()
                        Files.isRegularFile(p) && FAILED_DATA_FILE_PATTERN.matches(name)
                    }
                    .sorted()
                    .forEach { path: Path ->
                        val success = runCatching { handler.handleFailedData(path.toFile()) }
                            .onFailure { logger.error(it, "Error handling file $path") }
                            .getOrDefault(false)
                        if (success) {
                            runCatching {
                                Files.delete(path)
                                logger.info("Deleted file $path")
                            }.onFailure { logger.error(it, "Failed to delete file $path") }
                        }
                    }
            }
            val noRegularFilesLeft = Files.list(dir).use { s ->
                !s.anyMatch { path: Path -> Files.isRegularFile(path) }
            }
            if (noRegularFilesLeft) {
                Files.delete(dir)
                logger.info("Deleted empty directory $dir")
            }
        } catch (e: IOException) {
            logger.error(e, "Scanning directory $dir error")
        }
    }

    /** Logger */
    private val logger = LogFactory.getLog(this::class)

    companion object {
        /** Failed-data retry lock key prefix (historical typo `faile-data-retry` has been corrected). */
        private const val FAILED_DATA_RETRY_LOCK_PREFIX = "failed-data-retry-"

        /**
         * Regex for valid failed-data file names: `{timestamp}-{UUID}.json`.
         * Used to filter out non-data files inside the directory (such as IDE `.DS_Store` files or temporary swap files).
         */
        private val FAILED_DATA_FILE_PATTERN = Regex("""\d+-[0-9a-fA-F\-]+\.json""")
    }
}
