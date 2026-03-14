package io.kudos.ms.sys.api.internal.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * sys-api-provider自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.sys.api.internal"])
open class SysApiProviderAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-sys-api-internal"

}