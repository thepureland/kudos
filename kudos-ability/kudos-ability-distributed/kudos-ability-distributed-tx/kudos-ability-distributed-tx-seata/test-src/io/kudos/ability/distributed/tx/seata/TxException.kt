package io.kudos.ability.distributed.tx.seata

/**
 * 模拟事务异常
 *
 * @author will
 * @since 5.1.1
 */
class TxException(msg: String?) : RuntimeException(msg)
