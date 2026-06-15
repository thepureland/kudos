package io.kudos.ability.distributed.lock.common.exception

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [DistributedLockAcquireException] 单元测试。
 *
 * 覆盖：lockKey 属性保存、异常消息内容、空串/Unicode/超长 key、RuntimeException 继承关系。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DistributedLockAcquireExceptionTest {

    @Test
    fun constructor_keepsLockKeyAndBuildsMessage() {
        val e = DistributedLockAcquireException("order:123")

        assertEquals("order:123", e.lockKey)
        assertEquals("Failed to acquire distributed lock: order:123", e.message)
    }

    @Test
    fun isRuntimeException() {
        assertIs<RuntimeException>(DistributedLockAcquireException("k"))
    }

    @Test
    fun emptyLockKey_isAllowed() {
        val e = DistributedLockAcquireException("")

        assertEquals("", e.lockKey)
        assertEquals("Failed to acquire distributed lock: ", e.message)
    }

    @Test
    fun unicodeLockKey_isPreservedInMessage() {
        val key = "订单锁🔒:用户#42"
        val e = DistributedLockAcquireException(key)

        assertEquals(key, e.lockKey)
        assertTrue(e.message!!.endsWith(key))
    }

    @Test
    fun veryLongLockKey_isPreserved() {
        val key = "k".repeat(10_000)
        val e = DistributedLockAcquireException(key)

        assertEquals(key, e.lockKey)
        assertTrue(e.message!!.endsWith(key))
    }

}
