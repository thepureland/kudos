package io.kudos.ms.sys.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * Auto-configuration class for the sys atomic service.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.sys.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class SysAutoConfiguration : IComponentInitializer {


    override fun getComponentName() = "kudos-ms-sys-core"

}