package io.kudos.ability.web.springmvc.init

import io.kudos.ability.security.enforcement.filter.AdminAuthorizationFilter
import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.ability.security.enforcement.port.ITrustedAuthorizationContextProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/** Every Spring MVC admin surface requires a real authorization decision backend. */
@Configuration(proxyBeanMethods = false)
open class AdminAuthorizationConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = ["adminAuthorizationFilterRegistration"])
    open fun adminAuthorizationFilterRegistration(
        decisions: ObjectProvider<IAuthzDecisionProvider>,
        registry: ObjectProvider<IPermissionPointRegistry>,
        properties: ObjectProvider<EnforcementProperties>,
        freshness: ObjectProvider<ITokenFreshnessValidator>,
        trustedContexts: ObjectProvider<ITrustedAuthorizationContextProvider>,
    ): FilterRegistrationBean<AdminAuthorizationFilter> = FilterRegistrationBean(
        AdminAuthorizationFilter(
            { decisions.getIfAvailable() },
            { registry.getIfAvailable() },
            { properties.getIfAvailable() ?: EnforcementProperties() },
            { freshness.getIfAvailable() },
            { trustedContexts.orderedStream().toList() },
        ),
    ).apply {
        // Login/session filters populate the trusted subject before the admin gate runs.
        order = Ordered.LOWEST_PRECEDENCE - 50
        addUrlPatterns("/*")
    }
}
