package io.kudos.ms.msg.api.internal.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * msg-api-provider entrypoint
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudos
//region your codes 1
class MsgApiProviderApplication
//endregion your codes 1

fun main(args : Array<String>) {
    //region your codes 2

    //endregion your codes 2
    SpringApplication.run(MsgApiProviderApplication::class.java, *args)
}
