package io.kudos.ability.distributed.stream.rabbit.main

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.bind.annotation.RequestParam

/**
 * RabbitMQ producer client (interface client; target set by the test via spring.http.serviceclient).
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRabbitMqProducerClient {

    @GetExchange("/producer/send")
    fun send(@RequestParam("message") message: String?)

}
