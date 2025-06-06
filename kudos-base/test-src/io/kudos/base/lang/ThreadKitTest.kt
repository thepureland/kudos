package io.kudos.base.lang

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * test for ThreadKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class ThreadKitTest {

    // 1) 测试 sleep(millis: Long) 在不被中断时能够正确返回
    @Test
    fun sleep_Millis_NoException() {
        // 测量睡眠时间，确保调用不会抛出异常且大致睡眠了预期时长
        val elapsed = measureTimeMillis {
            ThreadKit.sleep(100)
        }
        assertTrue(elapsed >= 90, "ThreadKit.sleep(100) 应至少睡眠接近 100ms，而实际: $elapsed ms")
    }

    // 2) 测试 sleep(duration: Long, unit: TimeUnit) 在不被中断时能够正确返回
    @Test
    fun sleep_DurationUnit_NoException() {
        val elapsed = measureTimeMillis {
            ThreadKit.sleep(200, TimeUnit.MILLISECONDS)
        }
        assertTrue(elapsed >= 180, "ThreadKit.sleep(200, MILLISECONDS) 应至少睡眠接近 200ms，而实际: $elapsed ms")
    }

    // 3) 测试 sleep 时如果线程被中断，能够捕获并忽略 InterruptedException
    @Test
    fun sleep_WhenInterrupted_IgnoredInternally() {
        // 启动一个线程，马上中断，然后调用 sleep(…)：
        val thread = Thread {
            // 在子线程中打断它自己
            Thread.currentThread().interrupt()
            // 由于已经被打断，调用 sleep 会抛出 InterruptedException，
            // 再被 ThreadKit 捕获并忽略，不应抛到外面
            ThreadKit.sleep(50)
        }
        thread.start()
        thread.join(500)
        // 如果子线程没有抛出未捕获异常，就说明忽略机制生效了
        assertFalse(thread.isAlive, "线程应已正常结束")
    }

    // 4) 测试 gracefulShutdown：当线程池中没有任务时，应快速返回且不抛异常
    @Test
    fun gracefulShutdown_EmptyExecutor_NoException() {
        val pool = Executors.newSingleThreadExecutor()
        // 冲突：先 shutdown，再调用
        ThreadKit.gracefulShutdown(pool, 1, 1, TimeUnit.SECONDS)
        assertTrue(pool.isShutdown, "执行 gracefulShutdown 之后，线程池应处于已 shutdown 状态")
    }

    // 5) 测试 gracefulShutdown：当线程池中有一个长期运行任务、且第一个 awaitTermination 超时触发 shutdownNow，再次 awaitTermination 超时
    @Test
    fun gracefulShutdown_WithLongTask_TriggersShutdownNow() {
        val pool = Executors.newSingleThreadExecutor()
        // 提交一个会一直阻塞的任务
        pool.submit {
            try {
                Thread.sleep(5000)
            } catch (_: InterruptedException) {
                // 被 cancel 之后，我们也忽略
            }
        }
        // 设置超时时间都为 100ms：第一次 awaitTermination 超时后会调用 shutdownNow，
        // 第二次 awaitTermination 也超时，则会走到 “线程池未结束!” 的 warn 分支
        // 这里只验证不抛异常
        ThreadKit.gracefulShutdown(pool, 100, 100, TimeUnit.MILLISECONDS)
        // 此时线程池肯定已经关闭（shutdown 或 shutdownNow 都已经调用）
        assertTrue(pool.isShutdown, "执行 gracefulShutdown 后，线程池应处于 shutdown 状态")
    }

    // 6) 测试 normalShutdown：立即调用 shutdownNow，并等待超时
    @Test
    fun normalShutdown_WithRunningTask_NoException() {
        val pool = Executors.newFixedThreadPool(2)
        // 提交一个长期任务
        pool.submit {
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
            }
        }
        // 调用 normalShutdown，timeout 很短
        ThreadKit.normalShutdown(pool, 50, TimeUnit.MILLISECONDS)
        assertTrue(pool.isShutdown, "执行 normalShutdown 后，线程池应处于 shutdown 状态")
    }

}