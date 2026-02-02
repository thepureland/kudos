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
//region your codes 1
open class UserApiAdminAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "kudos-ms-user-api-admin"

}
