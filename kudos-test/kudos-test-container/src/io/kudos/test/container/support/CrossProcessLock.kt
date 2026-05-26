package io.kudos.test.container.support

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * File-based cross-process lock.
 *
 * Uses [java.nio.channels.FileLock] to serialize critical sections across multiple JVM processes on
 * <strong>the same machine</strong>, mitigating "detect-then-start" race conditions (for example,
 * multiple test processes contending for fixed host ports or for containers sharing a Docker label).
 *
 * Typical usage: the caller uses `synchronized` as a fast in-process path; on a miss, call [run]
 * which blocks until it acquires the exclusive lock on the default or custom lock file, then runs the
 * Docker / Testcontainers detection and startup logic under `jvmMonitor`.
 *
 * The lock file should live on a reliable local disk under `java.io.tmpdir` (or a path specified by
 * the caller via system property); avoid network file systems such as NFS that implement file locks
 * incompletely.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object CrossProcessLock {

    /**
     * Executes [criticalSection] under both the cross-process file lock (unless disabled) and
     * `synchronized(jvmMonitor)`.
     *
     * @param jvmMonitor non-null; the same monitor that the caller's other `synchronized` blocks use, to provide in-process mutual exclusion
     * @param lockPathProperty optional; if non-blank, reads this system property's string value as the lock file path
     * @param disableLockProperty optional; if non-blank and the system property is `true`, the file lock is not used and only `synchronized(jvmMonitor)` applies
     * @param defaultLockPath used when [lockPathProperty] is not configured or the corresponding property is empty
     * @param criticalSection executed while holding the lock and inside [jvmMonitor]
     * @return the return value of [criticalSection]
     */
    fun <T> run(
        jvmMonitor: Any,
        lockPathProperty: String?,
        disableLockProperty: String?,
        defaultLockPath: () -> Path,
        criticalSection: () -> T,
    ): T {
        if (isLockDisabled(disableLockProperty)) {
            synchronized(jvmMonitor) {
                return criticalSection()
            }
        }
        val lockPath = resolveLockPath(lockPathProperty, defaultLockPath)
        try {
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use {
                    synchronized(jvmMonitor) {
                        return criticalSection()
                    }
                }
            }
        } catch (e: IOException) {
            val hint = if (!disableLockProperty.isNullOrBlank()) {
                " (set -D$disableLockProperty=true to skip the file lock)"
            } else {
                ""
            }
            throw IllegalStateException("Failed to acquire cross-process mutex lock: $lockPath$hint", e)
        }
    }

    private fun isLockDisabled(disableLockProperty: String?): Boolean {
        if (disableLockProperty.isNullOrBlank()) return false
        return System.getProperty(disableLockProperty, "false").toBoolean()
    }

    private fun resolveLockPath(lockPathProperty: String?, defaultLockPath: () -> Path): Path {
        if (!lockPathProperty.isNullOrBlank()) {
            val value = System.getProperty(lockPathProperty)
            if (!value.isNullOrBlank()) {
                return Path.of(value.trim())
            }
        }
        return defaultLockPath()
    }

}
