package io.kudos.ms.msg.api.internal.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * msg-api-provider auto-configuration
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.msg.api.internal"])
//region your codes 1
open class MsgApiProviderAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "kudos-ms-msg-api-internal"

}
