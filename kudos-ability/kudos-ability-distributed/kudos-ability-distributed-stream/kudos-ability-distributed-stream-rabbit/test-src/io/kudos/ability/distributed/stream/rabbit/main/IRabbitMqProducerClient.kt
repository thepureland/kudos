package io.kudos.ability.distributed.stream.rabbit.main

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * RabbitMq生产者Feign客户端
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "rabbit-ms-p", url = "localhost:53122")
interface IRabbitMqProducerClient {

    @RequestMapping("/producer/send")
    fun send(@RequestParam("message") message: String?)

}
