package io.kudos.ability.distributed.stream.kafka.main

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * kafka生产者Feign客户端
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "kafkamsp", url = "localhost:53322")
interface IKafkaProducerClient {

    @RequestMapping("/producer/send")
    fun send(@RequestParam("message") message: String?)

}
