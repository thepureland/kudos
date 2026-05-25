package io.kudos.test.container.support

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * 基于文件的跨进程锁。
 *
 * 通过 [java.nio.channels.FileLock] 在<strong>同一台机器</strong>上多个 JVM 进程之间串行执行临界区，
 * 减轻「同时检测再启动」类竞态（例如多个测试进程争用固定宿主端口或共享 Docker 标签容器）。
 *
 * 典型用法：外层用 `synchronized` 做本进程内快速路径；未命中时再调用 [run]，
 * 在默认或自定义的锁文件上阻塞获取独占锁后，在 `jvmMonitor` 下执行与 Docker / Testcontainers 相关的检测与启动逻辑。
 *
 * 锁文件应位于本机可靠磁盘上的 `java.io.tmpdir`（或调用方通过系统属性指定的路径），不宜放在 NFS 等实现不完整的网络文件系统上。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object CrossProcessLock {

    /**
     * 在跨进程文件锁（若未关闭）及 `synchronized(jvmMonitor)` 下执行 [criticalSection]。
     *
     * @param jvmMonitor 非 null；用于与调用方其它 `synchronized` 块使用同一监视器，保证进程内互斥
     * @param lockPathProperty 可选；非空时读取该系统属性的字符串值作为锁文件路径
     * @param disableLockProperty 可选；非空且系统属性为 `true` 时不使用文件锁，仅 `synchronized(jvmMonitor)` 执行
     * @param defaultLockPath 当未配置 [lockPathProperty] 或对应属性为空时使用
     * @param criticalSection 在持锁且进入 [jvmMonitor] 后执行
     * @return [criticalSection] 的返回值
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
                "（可设置 -D$disableLockProperty=true 跳过文件锁）"
            } else {
                ""
            }
            throw IllegalStateException("无法获取跨进程互斥锁: $lockPath$hint", e)
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
