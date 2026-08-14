package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.aop.DsChangeAspect
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceInterceptor
import io.kudos.ability.data.rdb.jdbc.aop.TenantDsChangeAspect
import io.kudos.ability.data.rdb.jdbc.datasource.DataSourceClearListener
import io.kudos.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.DsDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.datasource.HikariDataSourceMeterInitEvent
import io.kudos.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad
import io.kudos.ability.data.rdb.jdbc.datasource.RoutingCache
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary


/**
 * Auto-configuration entry point for the JDBC layer. Every bean of this module is declared here
 * explicitly (no component scanning), so `@ConditionalOnMissingBean` replacement works uniformly
 * for all of them.
 *
 * Wiring chain:
 *  - [DynamicDataSourceProperties]: baomidou's standard properties bean (prefix `spring.datasource.dynamic`).
 *  - [MultipleDataSourceProperties]: this framework's "package path -> data source key" configuration (prefix `kudos.ability.jdbc`).
 *  - [DynamicDataSourceInterceptor] + [dynamicDataSourceRoutingAdvisor]: the routing advice and the
 *    advisor applying it with the configurable pointcut (`kudos.ability.jdbc.routingPointcut`).
 *  - [DsChangeAspect] / [TenantDsChangeAspect]: annotation-driven forced switching.
 *  - [RoutingCache]: shared "_context resolution -> real dsKey" cache.
 *  - [DsDataSourceCreator]: enhanced baomidou DataSource creator (with the Seata autoCommit fix).
 *  - [DsContextProcessor]: dynamic-route -> real-data-source resolver.
 *  - [IDynamicDataSourceLoad]: when none is provided, [DefaultDynamicDataSourceLoad] is used as a placeholder.
 *
 * `@AutoConfigureAfter(ContextAutoConfiguration::class)`: ensures
 * [io.kudos.context.core.KudosContextHolder] is ready — routing depends on
 * tenant context information.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class JdbcAutoConfiguration : IComponentInitializer {

    /** baomidou dynamic data source properties bean. @ConfigurationProperties auto-binds `spring.datasource.dynamic.*` from yml. */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic")
    open fun dynamicDataSourceProperties() = DynamicDataSourceProperties()

    /** "package path -> data source key" configuration bean, corresponding to `kudos.ability.jdbc.*` in yml. */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.jdbc")
    open fun multipleDataSourceProperties() = MultipleDataSourceProperties()

    /** Shared resolution cache: read through by the routing interceptor, invalidated by [DsContextProcessor.refreshDatasource]. */
    @Bean
    @ConditionalOnMissingBean
    open fun routingCache() = RoutingCache()

    /** Dynamic data source routing advice. Applied through [dynamicDataSourceRoutingAdvisor], not annotation scanning. */
    @Bean
    @ConditionalOnMissingBean
    open fun dynamicDataSourceInterceptor() = DynamicDataSourceInterceptor()

    /**
     * Advisor binding [DynamicDataSourceInterceptor] to the configurable pointcut expression
     * ([MultipleDataSourceProperties.routingPointcut], default `within(*..biz..*)`).
     * Order -99 keeps it inner to the annotation aspects (-100): they must write `forcedDs`
     * into the ThreadLocal before this advice reads it.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["dynamicDataSourceRoutingAdvisor"])
    open fun dynamicDataSourceRoutingAdvisor(
        properties: MultipleDataSourceProperties,
        interceptor: DynamicDataSourceInterceptor,
    ): AspectJExpressionPointcutAdvisor {
        val advisor = AspectJExpressionPointcutAdvisor()
        advisor.expression = properties.routingPointcut
        advisor.advice = interceptor
        advisor.order = -99
        return advisor
    }

    /** Aspect handling [@DsChange][io.kudos.ability.data.rdb.jdbc.aop.DsChange]. @Lazy defers init to avoid circular dependencies with early-loaded beans. */
    @Bean
    @Lazy
    @ConditionalOnMissingBean
    open fun dsChangeAspect() = DsChangeAspect()

    /** Aspect handling [@TenantDsChange][io.kudos.ability.data.rdb.jdbc.aop.TenantDsChange]. */
    @Bean
    @Lazy
    @ConditionalOnMissingBean
    open fun tenantDsChangeAspect() = TenantDsChangeAspect()

    /**
     * Overriding implementation of baomidou's [DefaultDataSourceCreator]:
     * [DsDataSourceCreator]. `@Primary` causes baomidou's own creator to step
     * aside; this also injects the global dynamic.* configuration (lazy / p6spy /
     * seata mode / publicKey) into the creator.
     */
    @Primary
    @Bean
    @ConditionalOnMissingBean
    open fun dataSourceCreator(
        properties: DynamicDataSourceProperties,
        dataSourceCreators: List<DataSourceCreator>,
    ): DefaultDataSourceCreator {
        val creator = DsDataSourceCreator()
        creator.setCreators(dataSourceCreators)
        creator.setPublicKey(properties.publicKey)
        creator.setLazy(properties.lazy)
        creator.setP6spy(properties.p6spy)
        creator.setSeata(properties.seata)
        creator.setSeataMode(properties.seataMode)
        return creator
    }

    /** Context routing resolver bean. @Bean("dsContextProcessor") explicitly names it so other modules can inject by name. */
    @Bean("dsContextProcessor")
    @ConditionalOnMissingBean
    open fun dsContextProcessor() = DsContextProcessor()

    /** Default [IDynamicDataSourceLoad] implementation — all null. Production apps should implement and register their own to replace it. */
    @Bean("dynamicDataSourceLoad")
    @ConditionalOnMissingBean
    open fun dynamicDataSourceLoad(): IDynamicDataSourceLoad = DefaultDynamicDataSourceLoad()

    /** kudos component initializer name, used for [IComponentInitializer] logging and ordering. */
    override fun getComponentName() = "kudos-ability-data-rdb-jdbc"

    /**
     * Wires [HikariDataSourceMeterInitEvent] when Micrometer is on the classpath — replaces
     * baomidou's default `EncDataSourceInitEvent` (which is `@ConditionalOnMissingBean`). The
     * delegate retains the encryption flow, and HikariCP connection-pool metrics get exposed via
     * the application's [io.micrometer.core.instrument.MeterRegistry].
     *
     * Nested @Configuration so the surrounding [JdbcAutoConfiguration] still loads cleanly on
     * deployments without Micrometer; Spring Boot only loads this inner class when
     * `MeterRegistry` is resolvable, deferring the import of [HikariDataSourceMeterInitEvent].
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["io.micrometer.core.instrument.MeterRegistry"])
    open class MicrometerConfiguration {
        @Bean
        @ConditionalOnMissingBean(DataSourceInitEvent::class)
        open fun hikariDataSourceMeterInitEvent(): DataSourceInitEvent = HikariDataSourceMeterInitEvent()
    }

    /**
     * Registers [DataSourceClearListener] when `kudos-ability-cache-common` is on the classpath
     * (the source of [io.kudos.ability.cache.common.support.CacheCleanRegister]). Without the cache
     * module, this listener has nothing to hook onto, so the bean is simply not created.
     *
     * Nested @Configuration with string-based `@ConditionalOnClass` defers loading
     * [DataSourceClearListener] (which imports cache-common types) until cache-common is actually
     * present — apps without the cache module still load [JdbcAutoConfiguration] cleanly.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["io.kudos.ability.cache.common.support.ICacheCleanListener"])
    open class CacheCleanListenerConfiguration {
        @Bean
        @ConditionalOnMissingBean
        open fun dataSourceClearListener(processor: DsContextProcessor): DataSourceClearListener =
            DataSourceClearListener(processor)
    }
}
