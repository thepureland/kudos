package io.kudos.context.retry

import io.kudos.context.core.KudosContextHolder
import java.io.File

/**
 * Failed-data retry handler contract.
 *
 * Used to persist potentially failing side effects (MQ publishes, RPC calls, etc.) as files, which [FailedDataRetryScanner]
 * scans periodically per [cronExpression] and hands back to this handler for re-execution. This interface defines only the three
 * abstractions of "business type + persistence + retry"; file paths and atomic operations are handled by [AbstractFailedDataHandler] / [RetryConfig].
 *
 * @param T the carrier type of the failed data
 * @author K
 * @since 1.0.0
 */
interface IFailedDataHandler<T> {
    /**
     * Defines the business type, used to separate file directories (multiple failed-data types in the same process do not interfere with each other).
     */
    val businessType: String

    /**
     * Defines the retry CRON expression.
     */
    val cronExpression: String

    /**
     * Persists failed data locally and returns the written file path; returns null if persistence fails.
     *
     * @param data the failed data to save
     * @return the file path after persistence; null on persistence failure
     * @author K
     * @since 1.0.0
     */
    fun persistFailedData(data: T): String?

    /**
     * Handles a single persisted file when the scheduled task fires.
     * Implementations must ensure the file is cleaned up after a successful retry and retained for the next round on failure.
     *
     * @param file the persisted failed-data file
     * @return true if processing succeeded and the file has been cleaned up; false if further retries are needed
     * @author K
     * @since 1.0.0
     */
    fun handleFailedData(file: File): Boolean

    /**
     * Failed-data persistence root directory + atomic-service subdirectory.
     *
     * Delegates to [RetryConfig.pathFor] by default: configurable via the system property `kudos.retry.failed-data-path`
     * or the environment variable `KUDOS_RETRY_FAILED_DATA_PATH`; falls back to `${java.io.tmpdir}/kudos-failed-data` when unset,
     * which is safe across Windows / Linux / containers.
     *
     * The subdirectory uses [KudosContextHolder.getOrNull] to read the atomic service code — `getOrNull` (instead of `get`) is used to
     * avoid the implicit-creation side effect of [KudosContextHolder] on non-HTTP request threads (such as scheduled tasks).
     */
    fun filePath(): String = RetryConfig.pathFor(KudosContextHolder.getOrNull()?.atomicServiceCode)
}
