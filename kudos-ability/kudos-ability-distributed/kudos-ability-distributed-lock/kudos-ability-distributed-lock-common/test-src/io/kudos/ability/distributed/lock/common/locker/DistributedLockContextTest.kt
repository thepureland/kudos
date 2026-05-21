package io.kudos.ability.distributed.lock.common.locker

import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame


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
