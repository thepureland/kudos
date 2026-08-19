package io.kudos.ability.distributed.stream.kafka.main

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.bind.annotation.RequestParam

/**
 * Kafka producer client (interface client; target set by the test).
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IKafkaProducerClient {

    @GetExchange("/producer/send")
    fun send(@RequestParam("message") message: String?)

}
