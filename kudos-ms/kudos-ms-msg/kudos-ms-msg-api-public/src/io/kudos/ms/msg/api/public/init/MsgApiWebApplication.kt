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
class MsgApiWebApplication

fun main(args : Array<String>) {

    SpringApplication.run(MsgApiWebApplication::class.java, *args)
}
