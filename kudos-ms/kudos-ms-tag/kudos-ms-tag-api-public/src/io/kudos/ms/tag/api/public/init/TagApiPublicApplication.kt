package io.kudos.ms.tag.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

@EnableKudos
class TagApiPublicApplication

fun main(args: Array<String>) {
    SpringApplication.run(TagApiPublicApplication::class.java, *args)
}
