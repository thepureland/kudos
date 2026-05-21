package io.kudos.ability.distributed.tx.seata.feign

import feign.RequestInterceptor
import io.kudos.ability.distributed.tx.seata.init.SeataAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Seata 全局事务 XID 通过 Feign 跨服务传播的自动装配。
 *
 * Apache Seata 2.x 自身**不带** Spring Cloud OpenFeign 集成：调用方发出 Feign 请求时不会
 * 自动把当前线程的 `RootContext.getXID()` 加到请求头，被调用方收到请求后也不会自动 bind
 * 回当前线程的 RootContext，结果就是跨服务的 `@GlobalTransactional` 拿不到分支，回滚什么
 * 都回滚不了。本模块自带一对协同组件解决这个：
 *  - [SeataFeignXidProcessor]：出站方向，把 `RootContext.getXID()` 写到请求头 `TX_XID`
 *    （`RootContext.KEY_XID`）。挂在 `IFeignRequestContextProcess` 扩展点上，
 *    `GlobalHeaderRequestInterceptor` 会循环调用所有此类处理器。
 *  - [SeataXidServletFilter]：入站方向，读 `TX_XID` 头并 `RootContext.bind`，请求结束
 *    `unbind`。`HIGHEST_PRECEDENCE` 确保任何 `@Transactional` 切面之前完成 bind。
 *
 * 仅当 classpath 同时具备 Feign (`feign.RequestInterceptor`) 与 Spring MVC 的
 * [OncePerRequestFilter] 时这套装配才激活；纯 Seata（无 Feign / 无 servlet 容器）的
 * 部署不会被影响。
 */
@Configuration
@AutoConfigureAfter(SeataAutoConfiguration::class)
@ConditionalOnClass(RequestInterceptor::class, OncePerRequestFilter::class)
open class SeataFeignXidAutoConfiguration : IComponentInitializer {

    @Bean
    open fun seataFeignXidProcessor(): SeataFeignXidProcessor = SeataFeignXidProcessor()

    @Bean
    open fun seataXidServletFilterRegistration(): FilterRegistrationBean<SeataXidServletFilter> =
        FilterRegistrationBean<SeataXidServletFilter>().apply {
            setFilter(SeataXidServletFilter())
            addUrlPatterns("/*")
            order = Ordered.HIGHEST_PRECEDENCE
        }

    override fun getComponentName() = "kudos-ability-distributed-tx-seata-feign-xid"
}
