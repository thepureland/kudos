package io.kudos.ability.distributed.lock.redisson.atom

/**
 * 原子执行任务线程。
 *
 * @deprecated 模块内部已无引用，仅为历史外部反射 / 二进制兼容保留。
 */
@Deprecated(
    message = "No internal usage remains; prefer explicit lock APIs instead.",
    level = DeprecationLevel.WARNING
)
class AtomExecuteTask(runnable: Runnable?) : Thread(runnable) {
    var status: Boolean = false
        private set

    override fun run() {
        status = true
        super.run()
    }

    fun stopTask() {
        status = false
    }
}
