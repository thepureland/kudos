package io.kudos.ability.distributed.stream.kafka.main

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(value = "kafkamsp")
interface IKafkaProducerClient {
    @RequestMapping("/producer/send")
    fun send(@RequestParam("message") message: String?)
}
