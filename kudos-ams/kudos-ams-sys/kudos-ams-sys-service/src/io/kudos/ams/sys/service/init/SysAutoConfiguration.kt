package io.kudos.ams.sys.service.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * sys原子服务自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ams.sys.service"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class SysAutoConfiguration : IComponentInitializer {


    override fun getComponentName() = "kudos-ams-sys-service"

}