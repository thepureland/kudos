package io.kudos.ability.distributed.stream.rocketmq.main


/**
 * RocketMQ test service interface.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRocketMqMainService {

    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?

}
