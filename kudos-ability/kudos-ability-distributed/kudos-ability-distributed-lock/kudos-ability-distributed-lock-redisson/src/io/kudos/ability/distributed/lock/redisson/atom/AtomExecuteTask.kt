package io.kudos.ability.distributed.lock.redisson.atom

/**
 * Atomic execution task thread.
 *
 * @deprecated No internal usage remains; kept only for historical external reflection / binary compatibility.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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
