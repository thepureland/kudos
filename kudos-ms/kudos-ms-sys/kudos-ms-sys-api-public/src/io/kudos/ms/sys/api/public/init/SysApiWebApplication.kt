package io.kudos.ms.sys.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * sys-api-web startup entry point.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
class SysApiWebApplication

fun main(args : Array<String>) {

    SpringApplication.run(SysApiWebApplication::class.java, *args)
}