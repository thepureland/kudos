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
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Flyway 多数据源自动配置入口。
 *
 * 在 [JdbcAutoConfiguration] 完成（动态数据源就绪）之后装配；通过 `initMethod="migrate"`
 * 让 [FlywayMultiDataSourceMigrator] 在 Bean 创建完成时立即跑迁移。`kudos.ability.flyway.enabled=false`
 * 可整体禁用（只读副本 / 离线运维场景）。
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
open class FlywayAutoConfiguration : IComponentInitializer {

    /**
     * 多数据源迁移器 Bean。`initMethod = "migrate"` 让 Spring 在 Bean 创建后调用 [FlywayMultiDataSourceMigrator.migrate]
     * 触发实际迁移；任何模块迁移失败都会通过抛异常打断应用启动。
     */
    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean
    open fun flywayMultiDataSourceMigrator(): FlywayMultiDataSourceMigrator = FlywayMultiDataSourceMigrator()

    /**
     * 多数据源配置 Bean，对应 yml 里 `kudos.ability.flyway.*`。
     * 与 Spring Boot 自带的 `spring.flyway.*` 不重叠：本配置只决定"模块 → 数据源 key"的映射，
     * Flyway 自身行为（baseline / encoding / outOfOrder 等）走 [FlywayProperties]。
     */
    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.flyway")
    open fun flywayMultiDataSourceProperties(): FlywayMultiDataSourceProperties = FlywayMultiDataSourceProperties()

    /** kudos 组件初始化器名称，供 [IComponentInitializer] 的日志和顺序追踪使用。 */
    override fun getComponentName(): String = "kudos-ability-data-rdb-flyway"
}
