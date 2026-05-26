package io.kudos.ms.user.api.public.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * user-api-web auto-configuration class
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.user.api.public"])
open class UserApiWebAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-user-api-public"

}
