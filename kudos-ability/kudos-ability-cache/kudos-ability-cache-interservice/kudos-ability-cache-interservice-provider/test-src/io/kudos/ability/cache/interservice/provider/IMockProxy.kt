package io.kudos.ability.cache.interservice.provider

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "inter-service-test", url = "localhost:8880")
interface IMockProxy {

    @GetMapping("/same")
    fun same(): RequestResult

    @GetMapping("/different1")
    fun different1(): RequestResult

    @GetMapping("/different2")
    fun different2(): RequestResult

}