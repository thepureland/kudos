package io.kudos.ability.distributed.tx.seata.init

import io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource


/**
 * Seata自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(JdbcAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-distributed-tx-seata.yml"],
    factory = YamlPropertySourceFactory::class
)
open class SeataAutoConfiguration : IComponentInitializer {

    /**
     * Seata 数据源代理实现——覆盖 jdbc 模块默认的 `IDataSourceProxy` bean。
     * `@Primary` 让多 bean 同时存在时本实现胜出，把 DataSource 包成 `DataSourceProxy`(AT)
     * 或 `DataSourceProxyXA`(XA)，让 ConnectionProxy 能在每条 SQL commit 时写 undo log /
     * 注册 BranchRegister。
     *
     * 注：旧实现还有一段构造 Ktorm `Database` 的注释代码，是双重代理的反面教材（让 Spring TX
     * 用的 DataSource 与 Ktorm 用的不是同一实例 → 分支注册失败）。已删除；正确的接线是让
     * `KudosContextHolder.currentDatabase()` 直接用 `Database.connectWithSpringSupport(dataSource)`，
     * 见 `kudos-ability-data-rdb-ktorm` README 的 "Database 与 DataSource 同实例约定"。
     */
    @Primary
    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    open fun dataSourceProxy(): IDataSourceProxy = SeataDataSourceProxy()

    override fun getComponentName() = "kudos-ability-distributed-tx-seata"

}
