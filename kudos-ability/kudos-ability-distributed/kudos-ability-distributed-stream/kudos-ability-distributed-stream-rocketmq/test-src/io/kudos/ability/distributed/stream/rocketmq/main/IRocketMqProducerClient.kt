package io.kudos.ability.distributed.stream.rocketmq.main

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.bind.annotation.RequestParam

/**
 * RocketMQ producer client (interface client; target set by the test via spring.http.serviceclient).
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
interface IRocketMqProducerClient {

    @GetExchange("/producer/send")
    fun send(@RequestParam("message") message: String?)

}
