package io.kudos.ability.distributed.lock.redisson.atom

import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.base.lang.ThreadKit
import io.kudos.base.logger.LogFactory
import jakarta.annotation.PreDestroy
import org.redisson.api.RLock
import java.util.concurrent.TimeUnit

/**
 * 基础原子服务
 * 分布式部署，可替代zookeeper选择主节点.
 * 即：使用redisson实现的主节点分配功能
 *
 * 抢到主节点后，就会开启守护线程，一直执行daemonProcess方法
 *
 * @author hanson
 * @since 1.0.0
 */
abstract class BaseAtomService {
    protected var log = LogFactory.getLog(this)
    private var run = false
    private var atomTask: AtomExecuteTask? = null

    fun start() {
        val thread = Thread(Runnable { this.masterNodeComp() })
        thread.setName(atomName() + "-竞争")
        thread.start()
    }

    private fun masterNodeComp() {
        this.run = true
        while (run) {
            var hasLock = false
            val lock: RLock = RedissonLockKit.getLock(atomKey())
            try {
                log.info("{0}-检测是否能成为主节点..", atomName())
                if (lock.tryLock(2, TimeUnit.SECONDS)) {
                    log.info("{0}-成为主节点...", atomName())
                    hasLock = true
                    //当前节点抢到锁了
                    while (run) {
                        if (this.atomTask == null) {
                            doStartDaemon()
                        }
                        log.info("{0}-主节点心跳续约..", atomName())
                        //为lock续约30S
                        lock.lock(30, TimeUnit.SECONDS)
                        ThreadKit.sleep(20000) // 每 20 秒续约一次
                    }
                } else {
                    //如果主节点丢失，也就是非主节点，则停止当前的监听
                    stopServer()
                }
                ThreadKit.sleep(20000) //每20s抢一次主节点
            } catch (e: Exception) {
                log.error(e, "竞争{0}-启动业务失败", atomName())
                ThreadKit.sleep(20000)
            } finally {
                if (hasLock) {
                    lock.unlock()
                }
            }
        }
    }

    /**
     * 真正执行任务的处理
     *
     * @return 下次任务等待时间
     */
    protected abstract fun daemonProcess(): Long

    private fun doStartDaemon() {
        atomTask = AtomExecuteTask(Runnable {
            while (atomTask!!.status) {
                try {
                    val tmeMillis = daemonProcess()
                    ThreadKit.sleep(tmeMillis)
                } catch (e: Exception) {
                    log.error(e, "任务执行失败！")
                }
            }
            log.warn("任务执行完毕，即将停止...")
        })
        atomTask!!.setName(atomName())
        atomTask!!.start()
    }

    protected fun atomKey(): String {
        return "atom::server::" + javaClass.getName()
    }

    protected fun atomName(): String {
        return "main-atom"
    }

    protected fun stopServer() {
        if (this.atomTask != null) {
            this.atomTask!!.stopTask()
            this.atomTask = null
        }
    }

    @PreDestroy
    fun stop() {
        if (this.run) {
            this.run = false
        }
        stopServer()
    }
}
