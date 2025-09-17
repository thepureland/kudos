package io.kudos.ability.distributed.lock.redisson.atom

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
