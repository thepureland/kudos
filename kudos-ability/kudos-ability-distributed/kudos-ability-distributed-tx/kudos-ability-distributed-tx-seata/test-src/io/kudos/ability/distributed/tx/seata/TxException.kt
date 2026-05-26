package io.kudos.ability.distributed.tx.seata

/**
 * Simulated transaction exception.
 *
 * @author will
 * @since 5.1.1
 */
class TxException(msg: String?) : RuntimeException(msg)
