package io.kudos.ms.user.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * user-api-web启动入口
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
//region your codes 1
class UserApiWebApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(UserApiWebApplication::class.java, *args)
}
