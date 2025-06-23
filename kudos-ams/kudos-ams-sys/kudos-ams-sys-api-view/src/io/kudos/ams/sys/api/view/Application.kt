package io.kudos.ams.sys.api.view

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

@EnableKudos
class Application

fun main(args : Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}