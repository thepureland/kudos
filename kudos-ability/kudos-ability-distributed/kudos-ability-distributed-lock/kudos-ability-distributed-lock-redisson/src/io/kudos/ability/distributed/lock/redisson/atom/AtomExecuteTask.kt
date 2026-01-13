package io.kudos.ability.distributed.lock.redisson.atom

/**
 * 原子执行任务线程
 * 用于执行需要原子性保证的任务，提供任务状态跟踪功能
 */
class AtomExecuteTask(runnable: Runnable?) : Thread(runnable) {
    var status: Boolean = false
        private set

    override fun run() {
        this.status = true
        super.run()
    }

    fun stopTask() {
        this.status = false
    }
}
