package io.kudos.ability.distributed.client.feign.client

import io.kudos.ability.distributed.client.feign.PostResult
import io.kudos.ability.distributed.client.feign.PostParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

/**
 * openFeign客户端
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient("test-feign-service")
interface IFeignClient {

    @GetMapping("/testFeignService/get")
    fun get(): Boolean

    @PostMapping("/testFeignService/post")
    fun post(data: PostParam): PostResult

    @GetMapping("/testFeignService/exception")
    fun exception()

}
