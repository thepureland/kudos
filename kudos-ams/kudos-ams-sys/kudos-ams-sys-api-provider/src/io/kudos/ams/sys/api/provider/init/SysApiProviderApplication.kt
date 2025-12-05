package io.kudos.ams.sys.api.provider.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * sys-api-provider启动入口
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
//region your codes 1
class SysApiProviderApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(SysApiProviderApplication::class.java, *args)
}