package io.kudos.ability.security.enforcement.init

import io.kudos.ability.security.enforcement.aop.RequiresPermissionAspect
import io.kudos.ability.security.enforcement.filter.PermissionEnforcementFilter
import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered


/**
 * Wires URL-level and method-level permission enforcement.
 *
 * Gating is deliberately conservative on both axes:
 *
 *  - [ConditionalOnProperty] `kudos.ability.security.enforcement.enabled` (default false) — adding
 *    this module to a build must never change how an application behaves until someone says so;
 *  - [ConditionalOnBean] on the two ports — with no decision point on the classpath the filter would
 *    have nothing to ask, and a filter that cannot ask must not be the thing that decides.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "kudos.ability.security.enforcement",
    name = ["enabled"],
    havingValue = "true",
)
@EnableConfigurationProperties(EnforcementProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-security-enforcement.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class EnforcementAutoConfiguration : IComponentInitializer {

    /**
     * Registered high in the chain — an unauthorized request should be turned away before the
     * application spends work on it — but after authentication, which is what populates the subject
     * the decision point reads.
     */
    @Bean
    @ConditionalOnBean(IAuthzDecisionProvider::class, IPermissionPointRegistry::class)
    @ConditionalOnMissingBean(name = ["permissionEnforcementFilterRegistration"])
    open fun permissionEnforcementFilterRegistration(
        decisionProvider: IAuthzDecisionProvider,
        registry: IPermissionPointRegistry,
        properties: EnforcementProperties,
        // Optional on purpose: a deployment with no freshness implementation still gets the filter,
        // simply without that check. ObjectProvider rather than @Autowired(required=false) so an
        // absent bean is a runtime null instead of a bean-resolution order question.
        tokenFreshnessValidator: org.springframework.beans.factory.ObjectProvider<ITokenFreshnessValidator>,
    ): FilterRegistrationBean<PermissionEnforcementFilter> {
        val registration = FilterRegistrationBean(
            PermissionEnforcementFilter(
                decisionProvider, registry, properties, tokenFreshnessValidator.getIfAvailable(),
            ),
        )
        registration.order = Ordered.LOWEST_PRECEDENCE - 100
        registration.addUrlPatterns("/*")
        return registration
    }

    @Bean
    @ConditionalOnBean(IAuthzDecisionProvider::class)
    @ConditionalOnMissingBean(RequiresPermissionAspect::class)
    open fun requiresPermissionAspect(
        decisionProvider: IAuthzDecisionProvider,
        properties: EnforcementProperties,
    ): RequiresPermissionAspect = RequiresPermissionAspect(decisionProvider, properties)

    override fun getComponentName() = "kudos-ability-security-enforcement"
}
