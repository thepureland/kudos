package io.kudos.ability.distributed.stream.rocketmq.producer

import io.kudos.ability.distributed.stream.rocketmq.data.RocketMqSimpleMsg


/**
 * RocketMQ test producer service interface.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRocketMqProducerService {

    fun producer(msg: RocketMqSimpleMsg)

}
