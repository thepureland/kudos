package io.kudos.ms.auth.api.admin.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * auth-api-admin启动入口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudos
class AuthApiAdminApplication

fun main(args : Array<String>) {

    SpringApplication.run(AuthApiAdminApplication::class.java, *args)
}
