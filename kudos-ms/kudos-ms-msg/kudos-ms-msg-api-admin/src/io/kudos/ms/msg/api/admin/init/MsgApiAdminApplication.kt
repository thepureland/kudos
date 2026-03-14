package io.kudos.ms.msg.api.admin.init

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication

/**
 * msg-api-admin entrypoint
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudos
class MsgApiAdminApplication

fun main(args : Array<String>) {

    SpringApplication.run(MsgApiAdminApplication::class.java, *args)
}
