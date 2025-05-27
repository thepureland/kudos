package io.kudos.ability.distributed.tx.seata.init

import jakarta.annotation.PostConstruct
import org.soul.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.soul.ability.distributed.tx.seata.SeataDatasourceProxy
import org.soul.ability.distributed.tx.seata.starter.SeataTxConfiguration
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource(value = ["classpath:soul-ability-distributed-tx-seata.yml"], factory = SoulPropertySourceFactory::class)
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class SeataAutoConfiguration {
    @Primary
    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    fun dataSourceProxy(): IDataSourceProxy {
        return SeataDatasourceProxy()
    }

    @PostConstruct
    fun init() {
        log.info("[soul-ability-distributed-tx-seata]初始化完成...")
    }

    companion object {
        private val log: Log = LogFactory.getLog(SeataTxConfiguration::class.java)
    }
}
