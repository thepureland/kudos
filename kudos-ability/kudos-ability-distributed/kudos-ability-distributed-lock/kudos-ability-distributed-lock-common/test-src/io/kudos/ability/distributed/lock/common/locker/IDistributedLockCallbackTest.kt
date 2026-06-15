package io.kudos.ability.distributed.lock.common.locker

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [IDistributedLockCallback] 单元测试。
 *
 * 覆盖：doLockSuccess 默认空实现（不抛异常、不产生副作用）、实现类可覆写默认方法、doLockFail 回调。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class IDistributedLockCallbackTest {

    @Test
    fun doLockSuccess_defaultImplementation_isNoOp() {
        val failedKeys = mutableListOf<String>()
        val callback = object : IDistributedLockCallback {
            override fun doLockFail(lockKey: String) {
                failedKeys += lockKey
            }
        }

        // 默认实现应安静返回，不抛异常，也不触发任何其它回调
        callback.doLockSuccess("any-key")
        callback.doLockSuccess("")

        assertEquals(emptyList(), failedKeys)
    }

    @Test
    fun doLockSuccess_canBeOverridden() {
        val succeededKeys = mutableListOf<String>()
        val callback = object : IDistributedLockCallback {
            override fun doLockSuccess(lockKey: String) {
                succeededKeys += lockKey
            }

            override fun doLockFail(lockKey: String) {}
        }

        callback.doLockSuccess("lock:a")
        callback.doLockSuccess("lock:b")

        assertEquals(listOf("lock:a", "lock:b"), succeededKeys)
    }

    @Test
    fun doLockFail_receivesLockKey() {
        var received: String? = null
        val callback = object : IDistributedLockCallback {
            override fun doLockFail(lockKey: String) {
                received = lockKey
            }
        }

        callback.doLockFail("lock:fail")

        assertEquals("lock:fail", received)
    }

}
