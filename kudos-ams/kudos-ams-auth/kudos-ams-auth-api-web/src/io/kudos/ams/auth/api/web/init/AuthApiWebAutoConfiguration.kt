package io.kudos.ams.auth.api.web.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * auth-api-web自动配置类
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ams.auth.api.web"])
//region your codes 1
open class AuthApiWebAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "kudos-ams-auth-api-web"

}
