package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.data.rdb.jdbc.aop.DynamicDataSourceAspect
import org.soul.ability.data.rdb.jdbc.datasource.*
import org.soul.ability.data.rdb.jdbc.starter.properties.MultipleDataSourceProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


/**
 * jdbc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
//@ComponentScan(
//    basePackages = [
//        "org.soul.ability.data.rdb.jdbc",
//    ],
//    excludeFilters = [ComponentScan.Filter(
//        type = FilterType.ASSIGNABLE_TYPE,
//        classes = [RdbJdbcConfiguration::class]
//    )]
//)
@AutoConfigureAfter(ContextAutoConfiguration::class)
//@EnableAutoConfiguration // 不然dynamic data source会找不到
//@ImportAutoConfiguration(DynamicDataSourceCreatorAutoConfiguration::class, DynamicDataSourceAutoConfiguration::class)
open class JdbcAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this)

//    @Bean("dataSourceProxy")
//    @ConditionalOnMissingBean
//    open fun dataSourceProxy(): IDataSourceProxy {
//        return DefaultDatasourceProxy()
//    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic")
    open fun dynamicDataSourceProperties() = DynamicDataSourceProperties()

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.jdbc")
    open fun multipleDataSourceProperties() = MultipleDataSourceProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun dynamicDataSourceAspect() = DynamicDataSourceAspect()

    @Primary
    @Bean
    @ConditionalOnMissingBean
    open fun dataSourceCreator(
        properties: DynamicDataSourceProperties,
        dataSourceCreators: MutableList<DataSourceCreator?>?,
        dataSourceInitEvent: DataSourceInitEvent?
    ): DefaultDataSourceCreator {
        val creator = DsDataSourceCreator()
        creator.setCreators(dataSourceCreators)
        creator.setDataSourceInitEvent(dataSourceInitEvent)
        creator.setPublicKey(properties.publicKey)
        creator.setLazy(properties.lazy)
        creator.setP6spy(properties.p6spy)
        creator.setSeata(properties.seata)
        creator.setSeataMode(properties.seataMode)
        return creator
    }

    @Bean("dsContextProcessor")
    @ConditionalOnMissingBean
    open fun dsContextProcessor() = DsContextProcessor()

    @Bean("dynamicDataSourceLoad")
    @ConditionalOnMissingBean
    open fun dynamicDataSourceLoad(): IDynamicDataSourceLoad = DefaultDynamicDataSourceLoad()

    @Bean
    @ConditionalOnMissingBean
    open fun dataSourceInitEvent(): DataSourceInitEvent = HikariDataSourceMeterInitEvent()

    @Bean
    @ConditionalOnMissingBean
    open fun dataSourceClearListener() = DataSourceClearListener()

    override fun getComponentName() = "kudos-ability-data-rdb-jdbc"

}