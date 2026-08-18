package io.kudos.ms.sys.api.internal.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * sys-api-provider startup entry point.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
class SysApiInternalApplication

fun main(args : Array<String>) {

    SpringApplication.run(SysApiInternalApplication::class.java, *args)
}