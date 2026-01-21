package io.kudos.ams.auth.api.provider.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * auth-api-provider启动入口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudos
//region your codes 1
class AuthApiProviderApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(AuthApiProviderApplication::class.java, *args)
}
