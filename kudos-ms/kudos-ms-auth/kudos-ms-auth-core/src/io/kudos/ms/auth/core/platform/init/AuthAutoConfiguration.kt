package io.kudos.ms.auth.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * Auto-configuration class for the auth atomic service.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class AuthAutoConfiguration : IComponentInitializer {


    override fun getComponentName() = "kudos-ms-auth-core"

}
