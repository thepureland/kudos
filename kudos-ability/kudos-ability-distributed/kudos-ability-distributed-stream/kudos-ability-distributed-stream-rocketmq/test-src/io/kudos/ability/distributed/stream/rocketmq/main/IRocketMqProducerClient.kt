package io.kudos.ability.distributed.stream.rocketmq.main

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * RocketMQ生产者Feign客户端
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "rocket-ms-p", url = "localhost:13542")
interface IRocketMqProducerClient {

    @RequestMapping("/producer/send")
    fun send(@RequestParam("message") message: String?)

}
