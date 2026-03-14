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
class MsgApiProviderApplication

fun main(args : Array<String>) {

    SpringApplication.run(MsgApiProviderApplication::class.java, *args)
}
