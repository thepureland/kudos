package io.kudos.ms.user.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * user-api-web application entry point
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
class UserApiWebApplication

fun main(args : Array<String>) {

    SpringApplication.run(UserApiWebApplication::class.java, *args)
}
