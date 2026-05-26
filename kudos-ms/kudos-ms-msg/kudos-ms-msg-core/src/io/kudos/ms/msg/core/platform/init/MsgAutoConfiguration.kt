package io.kudos.ms.msg.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * Auto-configuration class for the msg atomic service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.msg.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class MsgAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-msg-core"

}
