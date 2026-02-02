package io.kudos.ms.user.api.admin.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * user-api-admin启动入口
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
//region your codes 1
class UserApiAdminApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(UserApiAdminApplication::class.java, *args)
}
