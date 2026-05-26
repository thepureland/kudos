package io.kudos.ability.distributed.config.nacos.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration


/**
 * Nacos config-centre wiring entry — **note: this class registers no beans**.
 *
 * The actual Nacos client wiring is done by the `alibaba.cloud.nacos.config` starter
 * (spring-cloud-alibaba) through Spring Boot auto-configuration; this class only exists to:
 *  - give kudos's custom SPI scheduler `ComponentInitializerSelector` a hook to recognise this module
 *  - declare via `@AutoConfigureAfter(ContextAutoConfiguration::class)` that the kudos context must be ready first
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosConfigAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-config-nacos"

}