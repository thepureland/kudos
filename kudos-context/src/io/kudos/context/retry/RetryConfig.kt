package io.kudos.context.retry

import java.io.File

/**
 * Path configuration for the failed-data retry framework.
 *
 * **Refactoring motivation**: previously, [IFailedDataHandler.filePath] and the downstream `StreamProducerExceptionHandler.filePath()`
 * each hard-coded `/var/data/failed` —
 * - invalid path on Windows
 * - not writable in containers without a mounted volume
 * - two independent hard-codes, easy to miss one when changing
 *
 * They now resolve through this object uniformly. Priority order:
 * 1. System property `kudos.retry.failed-data-path`
 * 2. Environment variable `KUDOS_RETRY_FAILED_DATA_PATH`
 * 3. Default `${java.io.tmpdir}/kudos-failed-data` (cross-platform safe)
 *
 * The resolved value is read-only and does not change after application startup (`by lazy`, frozen after first access).
 *
 * @author K
 * @since 1.0.0
 */
object RetryConfig {

    /** System property key */
    const val SYS_PROP_BASE_PATH = "kudos.retry.failed-data-path"

    /** Environment variable key */
    const val ENV_VAR_BASE_PATH = "KUDOS_RETRY_FAILED_DATA_PATH"

    private const val DEFAULT_DIR_NAME = "kudos-failed-data"

    /**
     * Root directory for failed-data persistence.
     *
     * Note the use of `by lazy`: resolved by the priority order above on first access and cached afterward,
     * so **later modifications to the system property or environment variable have no effect**. This is intentional — it keeps the path consistent for the entire JVM lifetime.
     * When tests need to switch paths, call [resolveBasePath] directly.
     */
    val baseFailedDataPath: String by lazy { resolveBasePath() }

    /**
     * Resolves the root directory using the current priority order (for tests or explicit refresh scenarios).
     */
    internal fun resolveBasePath(): String {
        System.getProperty(SYS_PROP_BASE_PATH)?.takeIf { it.isNotBlank() }?.let { return it }
        System.getenv(ENV_VAR_BASE_PATH)?.takeIf { it.isNotBlank() }?.let { return it }
        val tmp = System.getProperty("java.io.tmpdir").trimEnd('/', '\\')
        return "$tmp${File.separator}$DEFAULT_DIR_NAME"
    }

    /**
     * Builds the persistence root directory for a specific atomic service.
     *
     * @param atomicServiceCode the atomic service code; may be null (e.g. non-HTTP request threads where the context is missing).
     *                          When null or blank, the placeholder subdirectory `"default"` is used to avoid building a dirty path such as `.../null`.
     */
    fun pathFor(atomicServiceCode: String?): String {
        val service = atomicServiceCode?.takeIf { it.isNotBlank() } ?: "default"
        return "$baseFailedDataPath${File.separator}$service"
    }
}
