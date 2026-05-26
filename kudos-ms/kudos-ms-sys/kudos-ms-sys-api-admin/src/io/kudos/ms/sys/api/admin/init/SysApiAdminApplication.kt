package io.kudos.ms.sys.api.admin.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * sys-api-admin startup entry point.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
class SysApiAdminApplication

fun main(args : Array<String>) {

    SpringApplication.run(SysApiAdminApplication::class.java, *args)
}