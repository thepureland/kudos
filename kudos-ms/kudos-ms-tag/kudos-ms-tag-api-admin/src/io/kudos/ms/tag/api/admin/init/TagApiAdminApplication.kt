package io.kudos.ms.tag.api.admin.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

@EnableKudos
class TagApiAdminApplication

fun main(args: Array<String>) {
    SpringApplication.run(TagApiAdminApplication::class.java, *args)
}
