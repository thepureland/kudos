package io.kudos.ability.cache.interservice.provider

import io.kudos.ability.cache.interservice.aop.ClientCacheable
import io.kudos.ability.cache.interservice.common.RequestResult
import io.kudos.base.lang.string.RandomStringKit
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 模拟微服务的Controller
 *
 * @author K
 * @since 1.0.0
 */
@RestController
open class MockMsController {

    @ClientCacheable
    @RequestMapping("/same")
    open fun same(): RequestResult {
        return RequestResult(1, "one")
    }


    //没加@ClientCacheable
    @RequestMapping("/different1")
    open fun different1(): RequestResult {
        return RequestResult(1, "one")
    }

    @ClientCacheable
    @RequestMapping("/different2")
    open fun different2(): RequestResult {
        return RequestResult(
            RandomStringKit.random(2, letters = false, numbers = true).toInt(),
            RandomStringKit.uuid(),
        )
    }

}