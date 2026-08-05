package io.kudos.ability.distributed.lock.common.locker

import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame


/**
 * [DistributedLockContext] 单元测试。
 *
 * 覆盖：set/get/clear 基本语义、未设置时 get 返回 null、set(null)、覆盖式 set、
 * 子线程不继承父线程回调（childValue 返回 null）、其它线程间隔离。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DistributedLockContextTest {

    @AfterTest
    fun teardown() {
        DistributedLockContext.clear()
    }

    @Test
    fun setAndGet_returnsCallbackInCurrentThread() {
        val callback = TestCallback()

        DistributedLockContext.set(callback)

        assertSame(callback, DistributedLockContext.get())
    }

    @Test
    fun clear_removesCurrentThreadCallback() {
        DistributedLockContext.set(TestCallback())

        DistributedLockContext.clear()

        assertNull(DistributedLockContext.get())
    }

    @Test
    fun get_withoutSet_returnsNull() {
        assertNull(DistributedLockContext.get())
    }

    @Test
    fun set_null_overwritesPreviousCallback() {
        DistributedLockContext.set(TestCallback())

        DistributedLockContext.set(null)

        assertNull(DistributedLockContext.get())
    }

    @Test
    fun set_again_replacesPreviousCallback() {
        DistributedLockContext.set(TestCallback())
        val second = TestCallback()

        DistributedLockContext.set(second)

        assertSame(second, DistributedLockContext.get())
    }

    @Test
    fun otherThread_setDoesNotLeakIntoCurrentThread() {
        thread(start = true) {
            DistributedLockContext.set(TestCallback())
        }.join()

        assertNull(DistributedLockContext.get())
    }

    @Test
    fun childThread_doesNotInheritParentCallback() {
        DistributedLockContext.set(TestCallback())
        val childValue = AtomicReference<IDistributedLockCallback?>()

        thread(start = true) {
            childValue.set(DistributedLockContext.get())
        }.join()

        assertNull(childValue.get())
    }

    private class TestCallback : IDistributedLockCallback {
        override fun doLockFail(lockKey: String) {}
    }

}
