package io.kudos.ability.data.rdb.flyway.init

import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Flyway自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(JdbcAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-data-rdb-flyway.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableConfigurationProperties(FlywayProperties::class)
open class FlywayAutoConfiguration : IComponentInitializer {

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean
    open fun flywayMultiDataSourceMigrator() = FlywayMultiDataSourceMigrator()

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.flyway")
    open fun flywayMultiDataSourceProperties() = FlywayMultiDataSourceProperties()


    override fun getComponentName() = "kudos-ability-data-rdb-flyway"

}