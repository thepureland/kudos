package io.kudos.ability.distributed.tx.seata.http

import io.kudos.ability.distributed.tx.seata.init.SeataAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Wires Seata XID propagation across services: the outbound half via [SeataXidRequestProcessor]
 * (registered as a bean, picked up by the interface client's context interceptor), the inbound half
 * via [SeataXidServletFilter].
 *
 * Both halves are needed for `@GlobalTransactional` to span services; either one alone leaves the
 * remote side outside the global transaction.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(SeataAutoConfiguration::class)
@ConditionalOnClass(ClientHttpRequestInterceptor::class, OncePerRequestFilter::class)
open class SeataXidPropagationAutoConfiguration : IComponentInitializer {

    @Bean
    open fun seataXidRequestProcessor(): SeataXidRequestProcessor = SeataXidRequestProcessor()

    @Bean
    open fun seataXidServletFilterRegistration(): FilterRegistrationBean<SeataXidServletFilter> =
        FilterRegistrationBean<SeataXidServletFilter>().apply {
            setFilter(SeataXidServletFilter())
            addUrlPatterns("/*")
            order = Ordered.HIGHEST_PRECEDENCE
        }

    override fun getComponentName() = "kudos-ability-distributed-tx-seata-xid"
}
