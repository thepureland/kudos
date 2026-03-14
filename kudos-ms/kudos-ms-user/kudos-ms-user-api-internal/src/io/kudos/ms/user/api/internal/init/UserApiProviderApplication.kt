package io.kudos.ms.user.api.internal.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * user-api-provider启动入口
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
class UserApiProviderApplication

fun main(args : Array<String>) {

    SpringApplication.run(UserApiProviderApplication::class.java, *args)
}
