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
class MsgApiPublicApplication

fun main(args : Array<String>) {

    SpringApplication.run(MsgApiPublicApplication::class.java, *args)
}
