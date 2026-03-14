package io.kudos.ms.auth.api.admin.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * auth-api-admin自动配置类
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.api.admin"])
open class AuthApiAdminAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-auth-api-admin"

}
