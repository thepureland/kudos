package io.kudos.ability.data.rdb.jdbc.init

import io.kudos.context.init.IComponentInitializer
import org.soul.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import org.soul.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad
import org.soul.ability.data.rdb.jdbc.starter.RdbJdbcConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType


/**
 * jdbc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(
    basePackages = [
        "org.soul.ability.data.rdb.jdbc",
        "io.kudos.ability.data.rdb.jdbc"
    ],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [RdbJdbcConfiguration::class]
    )]
)
//@AutoConfigureAfter(ContextAutoConfiguration::class)
//@EnableAutoConfiguration // 不然dynamic data source会找不到
//@ImportAutoConfiguration(DynamicDataSourceCreatorAutoConfiguration::class, DynamicDataSourceAutoConfiguration::class)
open class JdbcAutoConfiguration : IComponentInitializer {

//    @Bean("dataSourceProxy")
//    @ConditionalOnMissingBean
//    open fun dataSourceProxy(): IDataSourceProxy {
//        return DefaultDatasourceProxy()
//    }

    @Bean("dynamicDataSourceLoad")
    @ConditionalOnMissingBean
    open fun dynamicDataSourceLoad(): IDynamicDataSourceLoad = DefaultDynamicDataSourceLoad()

    override fun getComponentName() = "kudos-ability-data-rdb-jdbc"

}