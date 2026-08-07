package io.kudos.ability.security.enforcement.init

import io.kudos.ability.security.enforcement.aop.RequiresPermissionAspect
import io.kudos.ability.security.enforcement.filter.PermissionEnforcementFilter
import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.ability.security.enforcement.port.ITrustedAuthorizationContextProvider
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
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.core.env.Environment


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
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
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
    @ConditionalOnProperty(
        prefix = "kudos.ability.security.enforcement",
        name = ["enabled"],
        havingValue = "true",
    )
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
        trustedContextProviders: org.springframework.beans.factory.ObjectProvider<ITrustedAuthorizationContextProvider>,
    ): FilterRegistrationBean<PermissionEnforcementFilter> {
        val registration = FilterRegistrationBean(
            PermissionEnforcementFilter(
                decisionProvider,
                registry,
                properties,
                tokenFreshnessValidator.getIfAvailable(),
                trustedContextProviders.orderedStream().toList(),
            ),
        )
        registration.order = Ordered.LOWEST_PRECEDENCE - 100
        registration.addUrlPatterns("/*")
        return registration
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "kudos.ability.security.enforcement",
        name = ["enabled"],
        havingValue = "true",
    )
    @ConditionalOnBean(IAuthzDecisionProvider::class)
    @ConditionalOnMissingBean(RequiresPermissionAspect::class)
    open fun requiresPermissionAspect(
        decisionProvider: IAuthzDecisionProvider,
        properties: EnforcementProperties,
    ): RequiresPermissionAspect = RequiresPermissionAspect(decisionProvider, properties)

    /** Production must never silently run with authorization disabled or in observe-only mode. */
    @Bean
    open fun enforcementProductionSafetyCheck(
        properties: EnforcementProperties,
        environment: Environment,
    ): SmartInitializingSingleton = SmartInitializingSingleton {
        val production = environment.activeProfiles.any { active ->
            properties.productionProfiles.any { it.equals(active, ignoreCase = true) }
        }
        if (production && properties.failOnInsecureProduction) {
            check(properties.enabled) {
                "URL/method authorization enforcement is disabled in a production profile."
            }
            check(!properties.shadowMode) {
                "URL/method authorization enforcement is still in shadow mode in a production profile."
            }
        }
    }

    override fun getComponentName() = "kudos-ability-security-enforcement"
}
