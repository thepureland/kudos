package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceCreatorAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.EnableContext
import org.soul.ability.data.rdb.jdbc.datasource.DefaultDatasourceProxy
import org.soul.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import org.soul.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.soul.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad
import org.soul.ability.data.rdb.jdbc.starter.RdbJdbcConfiguration
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.*
import javax.annotation.PostConstruct


/**
 * jdbc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@EnableContext
@ComponentScan(
    basePackages = [
//        "com.baomidou.dynamic.datasource",
        "org.soul.ability.data.rdb.jdbc",
        "io.kudos.ability.data.rdb.jdbc"
    ],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [JdbcAutoConfiguration::class, RdbJdbcConfiguration::class]
    )]
)
@PropertySource(
    value = ["classpath:soul-ability-data-rdb-jdbc.yml"],
    factory = SoulPropertySourceFactory::class
)
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableAutoConfiguration
@ImportAutoConfiguration(DynamicDataSourceCreatorAutoConfiguration::class, DynamicDataSourceAutoConfiguration::class)
open class JdbcAutoConfiguration {

    private val logger = LoggerFactory.getLogger(this)

    @Bean("dataSourceProxy")
    @ConditionalOnMissingBean
    open fun dataSourceProxy(): IDataSourceProxy {
        logger.info("JdbcAutoConfiguration::dataSourceProxy()")
        return DefaultDatasourceProxy()
    }

    @Bean("dynamicDataSourceLoad")
    @ConditionalOnMissingBean
    open fun dynamicDataSourceLoad(): IDynamicDataSourceLoad {
        logger.info("JdbcAutoConfiguration::dynamicDataSourceLoad()")
        return DefaultDynamicDataSourceLoad()
    }

    @PostConstruct
    open fun init() {
        logger.info("【kudos-ability-data-rdb-jdbc】初始化完成.")
    }

}