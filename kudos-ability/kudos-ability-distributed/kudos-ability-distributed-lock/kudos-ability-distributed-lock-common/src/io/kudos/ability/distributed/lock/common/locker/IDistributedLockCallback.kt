package io.kudos.ability.distributed.lock.common.locker

/**
 * 分布式锁回调接口
 * 提供锁获取成功和失败时的回调处理
 */
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
