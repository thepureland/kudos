package io.kudos.ability.distributed.tx.seata.init

import io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import io.seata.rm.datasource.DataSourceProxy
import org.ktorm.database.Database
import org.ktorm.logging.Slf4jLoggerAdapter
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import javax.sql.DataSource


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
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
open class SeataAutoConfiguration : IComponentInitializer {

    private val log = LogFactory.getLog(this)

//    @Bean
//    open fun database(ds: DataSource, dsProxy: IDataSourceProxy): Database {
//        val logger = Slf4jLoggerAdapter("ktorm-sql")
//        return Database.connect(dataSource = dsProxy.proxyDatasource(ds), logger = logger)
//    }

    @Primary
    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    open fun dataSourceProxy(): IDataSourceProxy = SeataDataSourceProxy()

    override fun getComponentName() = "kudos-ability-distributed-tx-seata"

}
