package io.kudos.ability.distributed.tx.seata.init

import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import org.soul.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
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
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
open class SeataAutoConfiguration : IComponentInitializer {

    private val log = LogFactory.getLog(this)

    @Primary
    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    open fun dataSourceProxy(): IDataSourceProxy = SeataDatasourceProxy()

    override fun getComponentName() = "kudos-ability-distributed-tx-seata"

}
