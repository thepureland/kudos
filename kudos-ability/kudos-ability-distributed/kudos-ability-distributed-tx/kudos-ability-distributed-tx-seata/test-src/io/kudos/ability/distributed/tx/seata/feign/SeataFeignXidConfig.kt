package io.kudos.ability.distributed.tx.seata.feign

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 把 [SeataFeignXidProcessor] 和 [SeataXidServletFilter] 接入 Spring 容器。
 * 任何参与 Seata 全局事务且通过 Feign 跨服务调用的应用（包括 SeataTestBase
 * 启动的 sub-app）都需要 @Import 这个配置类。
 */
@Configuration
open class SeataFeignXidConfig {

    @Bean
    open fun seataFeignXidProcessor(): SeataFeignXidProcessor = SeataFeignXidProcessor()

    @Bean
    open fun seataXidServletFilterRegistration(): FilterRegistrationBean<SeataXidServletFilter> =
        FilterRegistrationBean<SeataXidServletFilter>().apply {
            setFilter(SeataXidServletFilter())
            addUrlPatterns("/*")
            order = 0
        }
}
