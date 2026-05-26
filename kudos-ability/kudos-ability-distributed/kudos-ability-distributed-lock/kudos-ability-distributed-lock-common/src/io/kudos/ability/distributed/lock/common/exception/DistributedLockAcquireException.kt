package io.kudos.ability.distributed.lock.common.exception

/**
 * Distributed lock acquisition failed.
 *
 * @property lockKey the business lock key that could not be acquired
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DistributedLockAcquireException(
    val lockKey: String
) : RuntimeException("Failed to acquire distributed lock: $lockKey")
