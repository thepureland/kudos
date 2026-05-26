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
 * Seata auto-configuration class.
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
     * Seata DataSource proxy implementation — overrides the default `IDataSourceProxy` bean from
     * the jdbc module. `@Primary` ensures this implementation wins when multiple beans coexist,
     * wrapping the DataSource as `DataSourceProxy` (AT) or `DataSourceProxyXA` (XA) so that the
     * ConnectionProxy can write undo logs / register a BranchRegister on every SQL commit.
     *
     * Note: the previous implementation contained commented-out code constructing a Ktorm
     * `Database`, a textbook example of double proxying (the DataSource used by Spring TX would
     * not match the one used by Ktorm -> branch registration fails). It has been removed; the
     * correct wiring is to let `KudosContextHolder.currentDatabase()` use
     * `Database.connectWithSpringSupport(dataSource)` directly, see the
     * "Database and DataSource share the same instance" convention in the `kudos-ability-data-rdb-ktorm` README.
     */
    @Primary
    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    open fun dataSourceProxy(): IDataSourceProxy = SeataDataSourceProxy()

    override fun getComponentName() = "kudos-ability-distributed-tx-seata"

}
