package io.kudos.ability.cache.interservice.client

import io.kudos.ability.cache.interservice.common.RequestResult
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

/**
 * 模拟FeignClient的代理，不用注册中心，直连
 *
 * @author K
 * @since  1.0.0
 */
@FeignClient(name = "inter-service-test", url = "localhost:13578")
interface IMockProxy {

    @GetMapping("/same")
    fun same(): RequestResult

    @GetMapping("/different1")
    fun different1(): RequestResult

    @GetMapping("/different2")
    fun different2(): RequestResult

}