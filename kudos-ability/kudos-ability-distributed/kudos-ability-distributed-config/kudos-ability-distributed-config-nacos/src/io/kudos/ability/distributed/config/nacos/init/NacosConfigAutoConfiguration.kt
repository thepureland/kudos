package io.kudos.ability.distributed.config.nacos.init

import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfiguration
import javax.annotation.PostConstruct


/**
 * Nacos配置中心自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@AutoConfiguration
open class NacosConfigAutoConfiguration: IComponentInitializer {



    @PostConstruct
    override fun init() {
        LoggerFactory.getLogger(this).info("【kudos-ability-distributed-config-nacos】初始化完成.")
    }

}