package io.kudos.ms.tag.api.internal.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

@EnableKudos
class TagApiInternalApplication

fun main(args: Array<String>) {
    SpringApplication.run(TagApiInternalApplication::class.java, *args)
}
