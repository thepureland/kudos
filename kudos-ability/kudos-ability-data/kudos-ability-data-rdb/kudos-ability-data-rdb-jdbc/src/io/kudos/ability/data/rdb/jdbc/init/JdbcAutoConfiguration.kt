package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceAspect
import io.kudos.ability.data.rdb.jdbc.datasource.DataSourceClearListener
import io.kudos.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.DsDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.datasource.HikariDataSourceMeterInitEvent
import io.kudos.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


/**
 * Auto-configuration entry point for the JDBC layer.
 *
 * Wiring chain:
 *  - [DynamicDataSourceProperties]: baomidou's standard properties bean (prefix `spring.datasource.dynamic`).
 *  - [MultipleDataSourceProperties]: this framework's "package path -> data source key" configuration (prefix `kudos.ability.jdbc`).
 *  - [DynamicDataSourceAspect]: routing aspect that intercepts `*..biz..*`.
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

    /** Dynamic data source routing aspect bean. Note: the pointcut is hard-coded to `within(*..biz..*)`; see the aspect comments. */
    @Bean
    @ConditionalOnMissingBean
    open fun dynamicDataSourceAspect() = DynamicDataSourceAspect()

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
