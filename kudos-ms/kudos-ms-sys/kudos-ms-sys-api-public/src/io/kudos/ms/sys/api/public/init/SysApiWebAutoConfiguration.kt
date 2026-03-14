package io.kudos.ms.sys.api.public.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * sys-api-web自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.sys.api.public"])
open class SysApiWebAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-sys-api-public"

}