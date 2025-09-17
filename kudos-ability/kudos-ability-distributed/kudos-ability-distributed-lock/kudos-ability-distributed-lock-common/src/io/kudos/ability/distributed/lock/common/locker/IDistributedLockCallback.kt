package io.kudos.ability.distributed.lock.common.locker

interface IDistributedLockCallback {
    /**
     * 成功上锁增加处理
     * @param lockKey
     */
    fun doLockSuccess(lockKey: String) {
    }

    /**
     * 上锁失败处理
     * @param lockKey
     */
    fun doLockFail(lockKey: String)
}
