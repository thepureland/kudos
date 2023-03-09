package io.kudos.base.lang

import org.soul.base.lang.ThreadTool
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * 线程相关工具类
 *
 * @author K
 * @since 1.0.0
 */
object ThreadKit {

    /**
     * 让当前线程休眠指定的毫秒数, 并忽略InterruptedException.
     *
     * @param millis 休眠的毫秒数
     * @author K
     * @since 1.0.0
     */
    fun sleep(millis: Long) {
        ThreadTool.sleep(millis)
    }

    /**
     * 让当前线程休眠指定的时间, 并忽略InterruptedException.
     *
     * @param duration 休眠的时间值
     * @param unit 休眠的时间单位
     * @author K
     * @since 1.0.0
     */
    fun sleep(duration: Long, unit: TimeUnit) {
        ThreadTool.sleep(duration, unit)
    }

    /**
     * 按照ExecutorService JavaDoc示例代码编写的Graceful Shutdown方法. 先使用shutdown, 停止接收新任务并尝试完成所有已存在任务.
     * 如果超时, 则调用shutdownNow, 取消在workQueue中Pending的任务,并中断所有阻塞函数. 如果仍超時，則強制退出.
     * 另对在shutdown时线程本身被调用中断做了处理.
     *
     * @param pool 线程池
     * @param shutdownTimeout 关闭超时时间
     * @param shutdownNowTimeout 现在关闭超时时间
     * @param timeUnit 时间单位
     * @author K
     * @since 1.0.0
     */
    fun gracefulShutdown(pool: ExecutorService, shutdownTimeout: Int, shutdownNowTimeout: Int, timeUnit: TimeUnit) {
        ThreadTool.gracefulShutdown(pool, shutdownTimeout, shutdownNowTimeout, timeUnit)
    }

    /**
     * 直接调用shutdownNow的方法, 有timeout控制. 取消在workQueue中Pending的任务, 并中断所有阻塞函数.
     *
     * @param pool 线程池
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @author K
     * @since 1.0.0
     */
    fun normalShutdown(pool: ExecutorService, timeout: Int, timeUnit: TimeUnit?) {
        ThreadTool.normalShutdown(pool, timeout, timeUnit)
    }

    /**
     * 当调用的方法栈里不含指定类名时，把栈信息打印到日志中.
     * 该方法可用于信息跟踪，如：跟踪资源是否被关闭。
     * 该方法只有在日志级别为DEBUG时才有效
     *
     * @param clazz 类
     * @author K
     * @since 1.0.0
     */
    fun printStackTraceOnNotCallByClass(clazz: KClass<*>) {
        ThreadTool.printStackTraceOnNotCallByClass(clazz.java)
    }

    /**
     * 抛出异常，打印方法调用栈日志.
     * 该方法只有在日志级别为DEBUG时才有效
     *
     * @author K
     * @since 1.0.0
     */
    fun printStackTrace() {
        ThreadTool.printStackTrace()
    }

    /**
     * 获取调用栈
     *
     * @return 调用栈
     * @author K
     * @since 1.0.0
     */
    fun getStackTrace(): Array<StackTraceElement> = ThreadTool.getStackTrace()

}