package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter
import io.kudos.ability.distributed.discovery.nacos.init.properties.NacosDiscoveryProperties
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Nacos 服务发现装配入口。
 *
 * 真正的 Nacos discovery 客户端装配交给 `alibaba.cloud.nacos.discovery` starter；
 * 本类只补充 kudos 自有的两件事：
 *
 *  1. 通过 [getComponentName] 把本模块挂到 kudos 自定义 SPI 调度器 `ComponentInitializerSelector`
 *  2. 注册 [FeignContextWebFilter]——Feign client 透传过来的 `TENANT_ID` / `TRACE_KEY` /
 *     `DATASOURCE_ID` 等 header **写回 provider 进程 `KudosContext`** 的唯一入口
 *
 * Filter 注册细节：
 *  - 仅在 `FilterRegistrationBean` 可用时装配（[ConditionalOnClass]），不影响非 servlet 应用
 *  - 排除开关 `kudos.ability.distributed.discovery.nacos.feign-context-filter.enabled=false`，
 *    供 dev 调试或对接非 kudos client 的过渡场景使用
 *  - order 设到尽量靠前（[FilterRegistrationBean.HIGHEST_PRECEDENCE] + 1）——上下文必须先于
 *    业务 filter / interceptor 就绪，否则下游拿到的 `KudosContextHolder` 内容是空的
 *  - urlPatterns 全路径（`/&#42;`）——filter 内部已用 `FEIGN_REQUEST` / `NOTIFY_REQUEST` 显式标记
 *    把普通浏览器 / curl 请求挡掉，不需要在注册侧再做路径白名单
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosDiscoveryAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-discovery-nacos"

    @Bean
    @ConditionalOnClass(FilterRegistrationBean::class)
    @ConditionalOnProperty(
        prefix = "kudos.ability.distributed.discovery.nacos.feign-context-filter",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun feignContextWebFilterRegistration(
        properties: NacosDiscoveryProperties = NacosDiscoveryProperties()
    ): FilterRegistrationBean<FeignContextWebFilter> {
        val registration = FilterRegistrationBean<FeignContextWebFilter>()
        registration.setFilter(FeignContextWebFilter(properties.feignContextFilter.allowUnmarkedContextHeaders))
        registration.addUrlPatterns("/*")
        registration.order = NacosDiscoveryProperties.FILTER_ORDER
        registration.setName("feignContextWebFilter")
        return registration
    }

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.discovery.nacos")
    open fun nacosDiscoveryProperties() = NacosDiscoveryProperties()

}
