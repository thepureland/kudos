package io.kudos.ms.auth.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * auth-api-web bootstrap entry point.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudos
class AuthApiPublicApplication

fun main(args : Array<String>) {

    SpringApplication.run(AuthApiPublicApplication::class.java, *args)
}
