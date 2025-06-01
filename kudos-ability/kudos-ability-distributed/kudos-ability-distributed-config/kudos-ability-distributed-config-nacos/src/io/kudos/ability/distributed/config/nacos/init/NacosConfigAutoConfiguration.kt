package io.kudos.ability.distributed.config.nacos.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration


/**
 * Nacos配置中心自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosConfigAutoConfiguration: IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-config-nacos"

}