package io.kudos.ms.auth.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceExceptionHandler
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.assurance.RequiresAuthenticationAssuranceAspect
import io.kudos.ms.auth.core.authentication.assurance.spi.IAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollmentProperties
import io.kudos.ms.auth.core.authentication.mfa.policy.TenantMfaPolicyProperties
import io.kudos.ms.auth.core.authentication.mfa.recovery.RecoveryCodeProperties
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.metrics.WebAuthnAuthenticatorRiskPolicyMetrics
import io.kudos.ms.auth.core.authentication.mfa.store.ITotpEnrollmentStore
import io.kudos.ms.auth.core.authentication.securityevent.policy.AuthSecurityEventSlaProperties
import io.kudos.ms.auth.core.authentication.securityevent.policy.AuthSecurityEventEscalationProperties
import io.kudos.ms.auth.core.authentication.securityevent.policy.DefaultAuthSecurityEventEscalationPolicy
import io.kudos.ms.auth.core.authentication.securityevent.policy.DefaultAuthSecurityEventSlaPolicy
import io.kudos.ms.auth.core.authentication.securityevent.policy.IAuthSecurityEventEscalationPolicy
import io.kudos.ms.auth.core.authentication.securityevent.policy.IAuthSecurityEventSlaPolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationProperties
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.IAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.PersistentAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.OnCallScheduleResponderResolver
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.spi.IAuthSecurityEventResponderResolver
import io.kudos.ms.auth.core.authentication.securityevent.notification.metrics.AuthSecurityEventNotificationMetrics
import io.kudos.ms.auth.core.authentication.mfa.store.InMemoryTotpEnrollmentStore
import io.kudos.ms.auth.core.authentication.mfa.store.RedisTotpEnrollmentStore
import io.kudos.ms.auth.core.authentication.session.store.IAuthenticationSessionStore
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.auth.core.authentication.session.store.RedisAuthenticationSessionStore
import io.kudos.ms.auth.core.authentication.store.IAuthenticationTransactionStore
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import io.kudos.ms.auth.core.authentication.store.RedisAuthenticationTransactionStore
import io.kudos.ms.auth.core.credential.password.init.PasswordHistoryProperties
import io.kudos.ms.auth.core.platform.authz.condition.DefaultConditionEvaluator
import io.kudos.ms.auth.core.platform.authz.condition.IConditionEvaluator
import io.kudos.ms.auth.core.platform.authz.condition.IConditionClauseEvaluator
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * Auto-configuration class for the auth atomic service.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class, RedisAutoConfiguration::class)
@EnableConfigurationProperties(
    PasswordHistoryProperties::class,
    TotpEnrollmentProperties::class,
    RecoveryCodeProperties::class,
    TenantMfaPolicyProperties::class,
    AuthSecurityEventSlaProperties::class,
    AuthSecurityEventEscalationProperties::class,
    AuthSecurityEventNotificationProperties::class,
)
open class AuthAutoConfiguration : IComponentInitializer {

    /** Default risk-based incident deadline policy, replaceable by tenant or industry-specific logic. */
    @Bean
    @ConditionalOnMissingBean(IAuthSecurityEventSlaPolicy::class)
    open fun authSecurityEventSlaPolicy(
        properties: AuthSecurityEventSlaProperties,
    ): IAuthSecurityEventSlaPolicy = DefaultAuthSecurityEventSlaPolicy(properties)

    /** Bounded default escalation cadence; industry deployments can replace the whole policy. */
    @Bean
    @ConditionalOnMissingBean(IAuthSecurityEventEscalationPolicy::class)
    open fun authSecurityEventEscalationPolicy(
        properties: AuthSecurityEventEscalationProperties,
    ): IAuthSecurityEventEscalationPolicy = DefaultAuthSecurityEventEscalationPolicy(properties)

    /**
     * Built-in on-call resolution from the tenant's stored rotation. Deployments that keep their rotation in
     * an external duty or paging system replace this bean; nothing vendor-specific belongs in core.
     */
    @Bean
    @ConditionalOnMissingBean(IAuthSecurityEventResponderResolver::class)
    open fun authSecurityEventResponderResolver(
        rosterService: IAuthSecurityEventOnCallRosterService,
    ): IAuthSecurityEventResponderResolver = OnCallScheduleResponderResolver(rosterService)

    /**
     * Persisted tenant routing, with the built-in assignee/security-queue rules as the fallback for tenants
     * that configured nothing. Still replaceable by a deployment resolving ticketing systems or other rules.
     */
    @Bean
    @ConditionalOnMissingBean(IAuthSecurityEventNotificationRoutePolicy::class)
    open fun authSecurityEventNotificationRoutePolicy(
        configService: IAuthSecurityEventNotificationRouteConfigService,
        responderResolver: IAuthSecurityEventResponderResolver,
    ): IAuthSecurityEventNotificationRoutePolicy =
        PersistentAuthSecurityEventNotificationRoutePolicy(configService, responderResolver)

    /** Default fail-closed ACR ordering, replaceable for an application's assurance taxonomy. */
    @Bean
    @ConditionalOnMissingBean(IAuthenticationAssurancePolicy::class)
    open fun authenticationAssurancePolicy(): IAuthenticationAssurancePolicy =
        DefaultAuthenticationAssurancePolicy()

    /** Evaluates declarative ACR and auth_time requirements against the request's trusted session. */
    @Bean
    @ConditionalOnMissingBean(AuthenticationAssuranceVerifier::class)
    open fun authenticationAssuranceVerifier(
        policy: IAuthenticationAssurancePolicy,
    ): AuthenticationAssuranceVerifier = AuthenticationAssuranceVerifier(policy)

    /** AOP execution point shared by public, admin and application-specific controllers. */
    @Bean
    @ConditionalOnMissingBean(RequiresAuthenticationAssuranceAspect::class)
    open fun requiresAuthenticationAssuranceAspect(
        verifier: AuthenticationAssuranceVerifier,
    ): RequiresAuthenticationAssuranceAspect = RequiresAuthenticationAssuranceAspect(verifier)

    /** Unified HTTP 403 challenge for assurance failures. */
    @Bean
    @ConditionalOnMissingBean(AuthenticationAssuranceExceptionHandler::class)
    open fun authenticationAssuranceExceptionHandler(): AuthenticationAssuranceExceptionHandler =
        AuthenticationAssuranceExceptionHandler()

    /**
     * The built-in condition evaluator, declared here rather than component-scanned so that
     * `@ConditionalOnMissingBean` is actually honoured — on a scanned `@Component` the condition is
     * evaluated before the application's own beans are known, and the override would not take.
     */
    @Bean
    @ConditionalOnMissingBean(IConditionEvaluator::class)
    open fun defaultConditionEvaluator(
        clauseEvaluators: ObjectProvider<IConditionClauseEvaluator>,
    ): IConditionEvaluator = DefaultConditionEvaluator(clauseEvaluators.orderedStream().toList())

    /** Shared transaction store used automatically when the Kudos Redis runtime is available. */
    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IAuthenticationTransactionStore::class)
    open fun redisAuthenticationTransactionStore(
        redisTemplates: RedisTemplates,
    ): IAuthenticationTransactionStore = RedisAuthenticationTransactionStore(redisTemplates)

    /** Single-node fallback for applications that intentionally run without Redis. */
    @Bean
    @ConditionalOnMissingBean(IAuthenticationTransactionStore::class)
    open fun authenticationTransactionStore(): IAuthenticationTransactionStore =
        InMemoryAuthenticationTransactionStore()

    /** Distributed, expiring pending TOTP enrollment storage when Redis is available. */
    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(ITotpEnrollmentStore::class)
    open fun redisTotpEnrollmentStore(redisTemplates: RedisTemplates): ITotpEnrollmentStore =
        RedisTotpEnrollmentStore(redisTemplates)

    /** Single-node pending enrollment fallback for deployments without Redis. */
    @Bean
    @ConditionalOnMissingBean(ITotpEnrollmentStore::class)
    open fun totpEnrollmentStore(): ITotpEnrollmentStore = InMemoryTotpEnrollmentStore()

    /** Distributed authentication-session registry used when Redis is available. */
    @Bean
    @ConditionalOnBean(RedisTemplates::class)
    @ConditionalOnMissingBean(IAuthenticationSessionStore::class)
    open fun redisAuthenticationSessionStore(
        redisTemplates: RedisTemplates,
    ): IAuthenticationSessionStore = RedisAuthenticationSessionStore(redisTemplates)

    /** Single-node authentication-session registry fallback. */
    @Bean
    @ConditionalOnMissingBean(IAuthenticationSessionStore::class)
    open fun authenticationSessionStore(): IAuthenticationSessionStore =
        InMemoryAuthenticationSessionStore()

    override fun getComponentName() = "kudos-ms-auth-core"

    /** Optional low-cardinality WebAuthn risk-policy metric when a registry is available. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["io.micrometer.core.instrument.MeterRegistry"])
    open class MicrometerConfiguration {
        @Bean
        @ConditionalOnBean(type = ["io.micrometer.core.instrument.MeterRegistry"])
        @ConditionalOnMissingBean(WebAuthnAuthenticatorRiskPolicyMetrics::class)
        open fun webAuthnAuthenticatorRiskPolicyMetrics(
            registry: io.micrometer.core.instrument.MeterRegistry,
        ): WebAuthnAuthenticatorRiskPolicyMetrics = WebAuthnAuthenticatorRiskPolicyMetrics(registry)

        @Bean
        @ConditionalOnBean(type = ["io.micrometer.core.instrument.MeterRegistry"])
        @ConditionalOnMissingBean(AuthSecurityEventNotificationMetrics::class)
        open fun authSecurityEventNotificationMetrics(
            registry: io.micrometer.core.instrument.MeterRegistry,
        ): AuthSecurityEventNotificationMetrics = AuthSecurityEventNotificationMetrics(registry)
    }

}
