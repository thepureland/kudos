package io.kudos.ms.msg.api.public.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * msg-api-web auto-configuration
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.msg.api.public"])
open class MsgApiWebAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-msg-api-public"

}
