package io.kudos.ability.data.rdb.flyway.init

import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * Auto-configuration entry point for Flyway multi-data-source migrations.
 *
 * Wired after [JdbcAutoConfiguration] (once dynamic data sources are ready); via `initMethod="migrate"`,
 * [FlywayMultiDataSourceMigrator] runs migrations as soon as the bean is created. Setting
 * `kudos.ability.flyway.enabled=false` disables everything (e.g. read-only replicas / offline ops).
 *
 * [FlywayPreConfiguration] + [FlywayModuleStrategy] are imported here to suppress Spring Boot's
 * default Flyway behavior (it would otherwise try to migrate `classpath:db/migration` against the
 * primary data source).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(JdbcAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-data-rdb-flyway.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableConfigurationProperties(FlywayProperties::class)
@ConditionalOnProperty(prefix = "kudos.ability.flyway", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Import(FlywayPreConfiguration::class)
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
     * Multi-data-source configuration bean, mapped to `kudos.ability.flyway.*` in yml.
     * Does not overlap with Spring Boot's `spring.flyway.*`: this config only governs the
     * "data source key → modules" mapping; Flyway's own behavior (baseline / encoding / outOfOrder, etc.)
     * is driven by [FlywayProperties].
     */
    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.flyway")
    open fun flywayMultiDataSourceProperties(): FlywayMultiDataSourceProperties = FlywayMultiDataSourceProperties()

    /**
     * No-op [FlywayMigrationStrategy] that prevents Spring Boot's default migration initializer
     * from running against the bare-bones Flyway bean defined in [FlywayPreConfiguration].
     */
    @Bean
    @ConditionalOnMissingBean(FlywayMigrationStrategy::class)
    open fun flywayMigrationStrategy(): FlywayMigrationStrategy = FlywayModuleStrategy()

    /** kudos component initializer name, used by [IComponentInitializer] for logging and order tracking. */
    override fun getComponentName(): String = "kudos-ability-data-rdb-flyway"
}
