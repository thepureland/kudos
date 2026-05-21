package io.kudos.ability.distributed.lock.common.exception

/**
 * 分布式锁获取失败。
 *
 * @property lockKey 未获取到的业务锁 key
 */
class DistributedLockAcquireException(
    val lockKey: String
) : RuntimeException("Failed to acquire distributed lock: $lockKey")
