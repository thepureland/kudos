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
class UserApiAdminApplication

fun main(args : Array<String>) {

    SpringApplication.run(UserApiAdminApplication::class.java, *args)
}
