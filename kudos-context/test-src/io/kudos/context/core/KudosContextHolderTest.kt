package io.kudos.context.core

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * KudosContextHolder 测试用例
 *
 * 覆盖：
 * - [KudosContextHolder.get] 自动创建语义（"懒初始化"，文档已注明）
 * - [KudosContextHolder.getOrNull] 与之相反，未初始化返回 null
 * - [KudosContextHolder.clear] 清理 ThreadLocal
 * - `InheritableThreadLocal` 传播到子线程
 * - 线程间相互隔离
 *
 * @author K
 * @since 1.0.0
 */
internal class KudosContextHolderTest {

    @BeforeTest
    fun resetBefore() = KudosContextHolder.clear()

    @AfterTest
    fun resetAfter() = KudosContextHolder.clear()

    // ============================================================
    // get vs getOrNull 语义差别
    // ============================================================

    @Test
    fun getOrNullReturnsNullWhenUninitialized() {
        assertNull(KudosContextHolder.getOrNull(), "未 set 前 getOrNull 应是 null")
    }

    @Test
    fun getCreatesContextOnFirstCallAndCachesIt() {
        // KDoc 明确：get() 在未初始化时自动创建并写入 ThreadLocal
        val first = KudosContextHolder.get()
        assertNotNull(first)
        val second = KudosContextHolder.get()
        assertSame(first, second, "二次调用应返回同一实例（已缓存）")
    }

    @Test
    fun getOrNullReturnsExistingAfterGetCreatesIt() {
        val created = KudosContextHolder.get()
        val read = KudosContextHolder.getOrNull()
        assertSame(created, read, "get() 创建后，getOrNull 能读到")
    }

    // ============================================================
    // set / clear
    // ============================================================

    @Test
    fun setReplacesCurrentContext() {
        val first = KudosContextHolder.get()
        val replacement = KudosContext()
        KudosContextHolder.set(replacement)
        assertSame(replacement, KudosContextHolder.getOrNull())
        assertNotSame(first, KudosContextHolder.getOrNull())
    }

    @Test
    fun clearRemovesContext() {
        KudosContextHolder.get()
        assertNotNull(KudosContextHolder.getOrNull())
        KudosContextHolder.clear()
        assertNull(KudosContextHolder.getOrNull(), "clear 后应回到未初始化")
    }

    // ============================================================
    // 跨线程行为：InheritableThreadLocal 应传播到子线程
    // ============================================================

    @Test
    fun contextPropagatesToChildThread() {
        val parentCtx = KudosContextHolder.get()
        val childCtx = AtomicReference<KudosContext?>()

        // 直接 new Thread，让 InheritableThreadLocal 在创建时拷贝
        val t = Thread {
            childCtx.set(KudosContextHolder.getOrNull())
        }
        t.start()
        t.join(2000)

        assertSame(parentCtx, childCtx.get(), "InheritableThreadLocal 应把父线程上下文传给子线程")
    }

    @Test
    fun contextIsIsolatedAcrossThreadsThatDontInherit() {
        // 用 ExecutorService 复用的现有线程：上下文不会被传过去
        val pool = Executors.newSingleThreadExecutor()
        // 先让 pool 的工作线程被创建（此时主线程 ThreadLocal 还是空的）
        pool.submit { /* warmup */ }.get(1, TimeUnit.SECONDS)

        // 现在主线程才创建上下文
        val parentCtx = KudosContextHolder.get()
        val workerCtx = AtomicReference<KudosContext?>()
        val done = CountDownLatch(1)
        pool.submit {
            workerCtx.set(KudosContextHolder.getOrNull())
            done.countDown()
        }
        assertTrue(done.await(2, TimeUnit.SECONDS))
        pool.shutdown()

        // 关键：worker 线程已存在（warmup 时创建），其 InheritableThreadLocal
        // 没有继承到主线程后来才 set 的值。这是已知陷阱——线程池场景下必须显式传递。
        assertNotSame(
            parentCtx,
            workerCtx.get(),
            "线程池里已存在的 worker 不会自动看到主线程后来 set 的上下文"
        )
    }

    @Test
    fun clearOnOneThreadDoesNotAffectAnother() {
        val mainCtx = KudosContextHolder.get()
        val otherSeen = AtomicReference<KudosContext?>()
        val phase1 = CountDownLatch(1)
        val phase2 = CountDownLatch(1)

        val t = Thread {
            // 子线程继承到 mainCtx
            val inherited = KudosContextHolder.getOrNull()
            otherSeen.set(inherited)
            phase1.countDown()
            // 等主线程 clear 后再看：子线程的引用不应被清掉
            assertTrue(phase2.await(2, TimeUnit.SECONDS))
            val afterMainClear = KudosContextHolder.getOrNull()
            assertSame(mainCtx, afterMainClear, "主线程 clear 不影响子线程")
        }
        t.start()
        assertTrue(phase1.await(2, TimeUnit.SECONDS))
        KudosContextHolder.clear()
        assertNull(KudosContextHolder.getOrNull(), "主线程已清")
        phase2.countDown()
        t.join(2000)

        assertSame(mainCtx, otherSeen.get())
    }
}
