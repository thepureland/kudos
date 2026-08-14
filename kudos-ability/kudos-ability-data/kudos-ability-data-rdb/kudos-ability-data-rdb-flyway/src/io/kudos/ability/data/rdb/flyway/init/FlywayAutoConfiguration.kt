package io.kudos.ability.data.rdb.flyway.init

import io.kudos.ability.data.rdb.flyway.multidatasource.DsContextFlywayDataSourceResolver
import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceProperties
import io.kudos.ability.data.rdb.flyway.multidatasource.IFlywayDataSourceResolver
import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Auto-configuration entry point for Flyway multi-data-source migrations.
 *
 * Wired after [JdbcAutoConfiguration] (once dynamic data sources are ready); via `initMethod="migrate"`,
 * [FlywayMultiDataSourceMigrator] runs migrations as soon as the bean is created. Setting
 * `kudos.ability.flyway.enabled=false` disables everything (e.g. read-only replicas / offline ops).
 *
 * This module depends on `flyway-core` only — Spring Boot's Flyway auto-configuration is not on the
 * classpath, so there is nothing to suppress (earlier versions needed a placeholder Flyway bean plus
 * a no-op migration strategy for that). As a safety net for classpaths that pull Boot's Flyway
 * starter in transitively, `kudos-ability-data-rdb-flyway.yml` sets `spring.flyway.enabled=false`,
 * which makes that auto-configuration back off entirely.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(JdbcAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-data-rdb-flyway.yml"],
    factory = YamlPropertySourceFactory::class
)
@ConditionalOnProperty(prefix = "kudos.ability.flyway", name = ["enabled"], havingValue = "true", matchIfMissing = true)
open class FlywayAutoConfiguration : IComponentInitializer {

    /**
     * Multi-data-source migrator bean. `initMethod = "migrate"` makes Spring invoke
     * [FlywayMultiDataSourceMigrator.migrate] after bean creation to trigger the actual migration;
     * any per-module migration failure interrupts application startup via an exception.
     */
    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean
    open fun flywayMultiDataSourceMigrator(): FlywayMultiDataSourceMigrator = FlywayMultiDataSourceMigrator()

    /**
     * Default [IFlywayDataSourceResolver] backed by the baomidou dynamic-routing table. Backs off
     * when the application registers its own resolver bean (the SPI that decouples this module
     * from any concrete multi-data-source implementation).
     */
    @Bean
    @ConditionalOnMissingBean(IFlywayDataSourceResolver::class)
    open fun flywayDataSourceResolver(): IFlywayDataSourceResolver = DsContextFlywayDataSourceResolver()

    /**
     * Configuration bean for everything under `kudos.ability.flyway.*`: the "data source key →
     * modules" mapping, execution order/mode, and the Flyway parameters themselves
     * (`flyway-config` + per-data-source `datasource-flyway-config`).
     */
    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.flyway")
    open fun flywayMultiDataSourceProperties(): FlywayMultiDataSourceProperties = FlywayMultiDataSourceProperties()

    /** kudos component initializer name, used by [IComponentInitializer] for logging and order tracking. */
    override fun getComponentName(): String = "kudos-ability-data-rdb-flyway"
}
