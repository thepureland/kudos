package io.kudos.ms.auth.api.internal.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * auth-api-provider startup entry point.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudos
class AuthApiInternalApplication

fun main(args : Array<String>) {

    SpringApplication.run(AuthApiInternalApplication::class.java, *args)
}
