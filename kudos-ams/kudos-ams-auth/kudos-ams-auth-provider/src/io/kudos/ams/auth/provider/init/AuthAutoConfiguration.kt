package io.kudos.ams.auth.provider.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * auth原子服务自动配置类
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ams.auth.provider"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class AuthAutoConfiguration : IComponentInitializer {


    override fun getComponentName() = "kudos-ams-auth-provider"

}
