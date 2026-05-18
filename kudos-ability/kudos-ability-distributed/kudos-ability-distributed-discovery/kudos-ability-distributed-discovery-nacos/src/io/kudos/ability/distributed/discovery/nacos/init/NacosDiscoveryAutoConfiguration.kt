package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration


/**
 * Nacos 服务发现装配入口——**注意：本类目前不注册任何 bean**。
 *
 * 真正的 Nacos discovery 客户端装配交给 `alibaba.cloud.nacos.discovery` starter；
 * 本类的存在仅为给 kudos 自定义 SPI 调度器一个识别本模块的入口（参 [getComponentName]）。
 *
 * **重要待修问题（见 README "已知限制"）**：
 * [io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter] 在历史上被
 * 注释停用。该 filter 是 Feign client 透传过来的 `TENANT_ID` / `TRACE_KEY` / `DATASOURCE_ID`
 * 等 header **写回当前请求的 `KudosContext`** 的唯一入口；它不被注册意味着 provider 端
 * **完全收不到 client 透传过来的上下文**（即 `kudos-ability-distributed-client-feign` 的工作
 * 在 provider 侧被丢弃）。当前不主动恢复注册是因为：(a) 行为改动影响所有下游 web 应用，
 * (b) 历史注释停用的原因 git log 没记录。需用户决策后再恢复。
 *
 * 启用方法：在本类增加一个返回 `FilterRegistrationBean<FeignContextWebFilter>` 的 @Bean 方法，
 * 内部 new 一个 FeignContextWebFilter()、addUrlPatterns 全路径（典型 `/` 加通配）、
 * order 设到 `FilterRegistrationBean.HIGHEST_PRECEDENCE + 1`。视情况加
 * `@ConditionalOnClass(FilterRegistrationBean::class)` 保护非 web 应用。
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosDiscoveryAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-discovery-nacos"

}
