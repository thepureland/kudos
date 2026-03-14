package io.kudos.ms.user.api.admin.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * user-api-admin自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.user.api.admin"])
open class UserApiAdminAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ms-user-api-admin"

}
