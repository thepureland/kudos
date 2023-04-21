package io.kudos.ability.web.springmvc

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/test")
open class TestController {

    @GetMapping("/get")
    open fun get(): String = "get"

}