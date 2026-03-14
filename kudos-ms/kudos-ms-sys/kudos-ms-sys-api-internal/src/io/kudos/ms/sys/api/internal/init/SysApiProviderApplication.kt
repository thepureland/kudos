package io.kudos.ms.sys.api.internal.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * sys-api-provider启动入口
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
class SysApiProviderApplication

fun main(args : Array<String>) {

    SpringApplication.run(SysApiProviderApplication::class.java, *args)
}