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
//region your codes 1
class AuthApiAdminApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(AuthApiAdminApplication::class.java, *args)
}
