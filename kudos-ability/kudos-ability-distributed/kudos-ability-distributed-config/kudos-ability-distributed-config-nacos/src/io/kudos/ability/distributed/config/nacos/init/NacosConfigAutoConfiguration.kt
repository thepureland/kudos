package io.kudos.ability.distributed.config.nacos.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration


/**
 * Nacos 配置中心装配入口——**注意：本类不注册任何 bean**。
 *
 * Nacos 客户端的实际装配由 `alibaba.cloud.nacos.config` starter（spring-cloud-alibaba）
 * 通过 Spring Boot 自动配置完成；本类的存在仅为：
 *  - 给 kudos 自定义的 SPI 调度器 `ComponentInitializerSelector` 一个识别本模块的入口
 *  - 通过 `@AutoConfigureAfter(ContextAutoConfiguration::class)` 声明 kudos 上下文应当先就绪
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosConfigAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-config-nacos"

}