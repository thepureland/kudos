package io.kudos.ability.distributed.client.feign.ms

import org.soul.base.bean.Pair //TODO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * openFeign调用的微服务的Controller
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
    fun post(data: Pair<Int?, String?>?): Pair<Int?, Boolean?> {
        return Pair<Int?, Boolean?>(1, true)
    }

    @GetMapping("/exception")
    fun exception() {
        throw RuntimeException("为了测试抛出的异常，可以忽略.")
    }

}
