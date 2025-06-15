package io.kudos.ability.cache.interservice.provider

import io.kudos.ability.cache.interservice.common.RequestResult
import io.kudos.base.lang.string.RandomStringKit
import org.soul.ability.cache.interservice.provider.ClientCacheable
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

    @RequestMapping("/different1")
    open fun different1(): RequestResult {
        return RequestResult(1, "one")
    }

    @ClientCacheable
    @RequestMapping("/different2")
    open fun different2(): RequestResult {
        return RequestResult(
            RandomStringKit.random(2, false, true).toInt(),
            RandomStringKit.uuid(),
        )
    }

}