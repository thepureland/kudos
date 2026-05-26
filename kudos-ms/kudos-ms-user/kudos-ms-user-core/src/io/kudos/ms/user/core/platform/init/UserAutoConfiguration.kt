package io.kudos.ms.user.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * User atomic service auto-configuration class.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.user.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class UserAutoConfiguration : IComponentInitializer {


    override fun getComponentName() = "kudos-ms-user-core"

}
