package io.kudos.test.container.support

import io.kudos.test.container.kit.TestContainerKit
import java.nio.file.Path

/**
 * 各测试容器 `start` 共用的跨进程文件锁：多 JVM 并行时串行化「检测 / 启动 Docker」，
 * 避免同一 [TestContainerKit.LABEL_KEY] 下重复起多套容器。
 *
 * 系统属性（[id] 为各容器约定的短名，如 `redis`、`nacos-seata`）：
 * `kudos.testcontainer.<id>.lock.file`、`kudos.testcontainer.<id>.lock.disable`。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object TestContainerCrossProcessLock {

    /**
     * @param monitorClass 进程内监视器，一般为对应 `XxxTestContainer` 类
     * @param id 锁与属性名中的短标识（小写、数字、连字符），如 `"kafka"`
     * @param criticalSection 持跨进程锁且在 `synchronized(monitorClass)` 下执行
     */
    fun <T> run(monitorClass: Class<*>, id: String, criticalSection: () -> T): T {
        val fileProp = "kudos.testcontainer.$id.lock.file"
        val disableProp = "kudos.testcontainer.$id.lock.disable"
        return CrossProcessLock.run(
            jvmMonitor = monitorClass,
            lockPathProperty = fileProp,
            disableLockProperty = disableProp,
            defaultLockPath = { Path.of(System.getProperty("java.io.tmpdir"), "kudos-testcontainer-$id.lock") },
            criticalSection = criticalSection,
        )
    }

}
