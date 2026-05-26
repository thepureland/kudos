package io.kudos.test.container.support

import io.kudos.test.container.kit.TestContainerKit
import java.nio.file.Path

/**
 * Cross-process file lock shared by each test container's `start`: serializes "detect / start Docker"
 * when multiple JVMs run in parallel, preventing duplicate containers being started under the same
 * [TestContainerKit.LABEL_KEY].
 *
 * System properties ([id] is the agreed short name for each container, e.g. `redis`, `nacos-seata`):
 * `kudos.testcontainer.<id>.lock.file`, `kudos.testcontainer.<id>.lock.disable`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object TestContainerCrossProcessLock {

    /**
     * @param monitorClass in-process monitor, typically the corresponding `XxxTestContainer` class
     * @param id short identifier used in lock and property names (lowercase, digits, hyphens), e.g. `"kafka"`
     * @param criticalSection executed while holding the cross-process lock and under `synchronized(monitorClass)`
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
