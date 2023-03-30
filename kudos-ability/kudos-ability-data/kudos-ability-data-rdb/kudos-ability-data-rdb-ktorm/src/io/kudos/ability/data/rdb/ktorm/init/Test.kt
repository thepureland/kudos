package io.kudos.ability.data.rdb.ktorm.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@EnableKudos
@SpringBootApplication
open class Test

fun main(args: Array<String>) {
    SpringApplication.run(Test::class.java, *args)
}