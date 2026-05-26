package io.kudos.ability.distributed.stream.rabbit.main


/**
 * RabbitMQ test service interface.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRabbitMqMainService {

    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?

}
