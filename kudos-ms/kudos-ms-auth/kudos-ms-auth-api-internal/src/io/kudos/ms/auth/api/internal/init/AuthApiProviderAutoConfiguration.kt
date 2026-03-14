package io.kudos.ms.auth.api.internal.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * auth-api-provider自动配置类
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.api.internal"])
open class AuthApiProviderAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-auth-api-internal"

}
