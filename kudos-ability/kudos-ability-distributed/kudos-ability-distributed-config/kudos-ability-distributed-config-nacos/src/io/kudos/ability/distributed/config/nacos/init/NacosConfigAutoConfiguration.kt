package io.kudos.ability.distributed.config.nacos.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfiguration


/**
 * Nacos配置中心自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@AutoConfiguration
open class NacosConfigAutoConfiguration: IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-config-nacos"

}