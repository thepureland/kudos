package io.kudos.ms.msg.api.admin.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * msg-api-admin auto-configuration
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.msg.api.admin"])
open class MsgApiAdminAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-msg-api-admin"

}
