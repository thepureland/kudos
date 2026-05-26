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
 * Auto-configuration that propagates the Seata global transaction XID across services via Feign.
 *
 * Apache Seata 2.x does **not** ship Spring Cloud OpenFeign integration: when the caller issues a
 * Feign request it does not automatically add the current thread's `RootContext.getXID()` to the
 * request headers, and the callee does not automatically bind it back to its own RootContext on
 * receipt. The result is that cross-service `@GlobalTransactional` cannot see the branches and
 * cannot roll anything back. This module ships a pair of cooperating components to fix that:
 *  - [SeataFeignXidProcessor]: outbound, writes `RootContext.getXID()` into the request header
 *    `TX_XID` (`RootContext.KEY_XID`). Hooks into the `IFeignRequestContextProcess` extension
 *    point; `GlobalHeaderRequestInterceptor` invokes every such processor in turn.
 *  - [SeataXidServletFilter]: inbound, reads the `TX_XID` header and calls `RootContext.bind`,
 *    then `unbind` at the end of the request. `HIGHEST_PRECEDENCE` ensures the bind happens
 *    before any `@Transactional` aspect.
 *
 * This wiring activates only when the classpath has both Feign (`feign.RequestInterceptor`) and
 * Spring MVC's [OncePerRequestFilter]; deployments running pure Seata (no Feign / no servlet
 * container) are unaffected.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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
