package io.kudos.ability.distributed.stream.kafka.main


/**
 * Kafka test service interface.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IKafkaMainService {

    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?

}
