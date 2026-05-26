package io.kudos.ability.distributed.stream.rabbit.producer

import io.kudos.ability.distributed.stream.rabbit.data.RabbitMqSimpleMsg


/**
 * RabbitMQ test producer service interface.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRabbitMqProducerService {

    fun producer(msg: RabbitMqSimpleMsg)

}
