package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceCreatorAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.data.rdb.jdbc.datasource.DefaultDatasourceProxy
import org.soul.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import org.soul.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.soul.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad
import org.soul.ability.data.rdb.jdbc.starter.RdbJdbcConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import javax.annotation.PostConstruct


/**
 * jdbc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
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
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableAutoConfiguration // 不然dynamic data source会找不到
@ImportAutoConfiguration(DynamicDataSourceCreatorAutoConfiguration::class, DynamicDataSourceAutoConfiguration::class)
open class JdbcAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    open fun dataSourceProxy(): IDataSourceProxy {
        return DefaultDatasourceProxy()
    }

    @Bean("dynamicDataSourceLoad")
    @ConditionalOnMissingBean
    open fun dynamicDataSourceLoad(): IDynamicDataSourceLoad {
        return DefaultDynamicDataSourceLoad()
    }

    @PostConstruct
    override fun init() {
        logger.info("【kudos-ability-data-rdb-jdbc】初始化完成.")
    }

}