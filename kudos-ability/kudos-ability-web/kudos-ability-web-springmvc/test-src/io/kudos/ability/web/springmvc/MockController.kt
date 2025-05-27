package io.kudos.ability.web.springmvc

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 模拟Controller
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/test")
open class MockController {

    @GetMapping("/get")
    open fun get(): String = "get"

}