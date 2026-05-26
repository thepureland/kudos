package io.kudos.ability.distributed.client.feign.ms

import io.kudos.ability.distributed.client.feign.PostParam
import io.kudos.ability.distributed.client.feign.PostResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Microservice controller invoked via OpenFeign.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/testFeignService")
class MockMsController {

    @GetMapping("/get")
    fun get(): Boolean {
        return true
    }

    @PostMapping("/post")
    fun post(data: PostParam?): PostResult {
        return PostResult(1, true)
    }

    @GetMapping("/exception")
    fun exception() {
        throw RuntimeException("Exception thrown for testing purposes; can be ignored.")
    }

}
