package io.kudos.ability.web.springmvc.server

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 测试用Controller
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/test")
class HelloWorldController {

    @GetMapping("/hello")
    fun getHelloWorld(): String {
        return "Hello World!"
    }

}