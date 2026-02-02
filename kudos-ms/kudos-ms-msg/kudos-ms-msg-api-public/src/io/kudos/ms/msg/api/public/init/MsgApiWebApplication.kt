package io.kudos.ms.msg.api.public.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * msg-api-web entrypoint
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudos
//region your codes 1
class MsgApiWebApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(MsgApiWebApplication::class.java, *args)
}
