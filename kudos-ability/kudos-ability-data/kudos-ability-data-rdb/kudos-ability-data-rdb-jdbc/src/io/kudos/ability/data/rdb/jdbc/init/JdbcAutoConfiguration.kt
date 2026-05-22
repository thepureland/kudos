package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceAspect
import io.kudos.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.DsDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


/**
 * JDBC 层自动配置入口。
 *
 * 装配链：
 *  - [DynamicDataSourceProperties]：baomidou 的标准属性 bean（前缀 `spring.datasource.dynamic`）
 *  - [MultipleDataSourceProperties]：本框架的"包路径 → 数据源 key"配置（前缀 `kudos.ability.jdbc`）
 *  - [DynamicDataSourceAspect]：拦截 `*..biz..*` 的路由切面
 *  - [DsDataSourceCreator]：增强版 baomidou DataSource 创建器（含 Seata autoCommit 修正）
 *  - [DsContextProcessor]：动态路由 → 真实数据源的解析器
 *  - [IDynamicDataSourceLoad]：未提供时用 [DefaultDynamicDataSourceLoad] 占位
 *
 * `@AutoConfigureAfter(ContextAutoConfiguration::class)`：确保 [io.kudos.context.core.KudosContextHolder]
 * 已就绪 —— 路由依赖租户上下文信息。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class JdbcAutoConfiguration : IComponentInitializer {

    /** baomidou 动态数据源属性 bean。@ConfigurationProperties 让 yml 里 `spring.datasource.dynamic.*` 自动绑定进来。 */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic")
    open fun dynamicDataSourceProperties() = DynamicDataSourceProperties()

    /** "包路径 → 数据源 key"配置 bean，对应 yml 里 `kudos.ability.jdbc.*`。 */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.jdbc")
    open fun multipleDataSourceProperties() = MultipleDataSourceProperties()

    /** 动态数据源路由切面 bean。注意：pointcut 写死 `within(*..biz..*)`，参考切面注释。 */
    @Bean
    @ConditionalOnMissingBean
    open fun dynamicDataSourceAspect() = DynamicDataSourceAspect()

    /**
     * baomidou [DefaultDataSourceCreator] 的覆盖实现 [DsDataSourceCreator]。`@Primary` 让
     * baomidou 自身的 creator 让位；这里同时把 dynamic.* 的全局配置（lazy / p6spy / seata
     * mode / publicKey）注入到 creator 里。
     */
    @Primary
    @Bean
    @ConditionalOnMissingBean
    open fun dataSourceCreator(
        properties: DynamicDataSourceProperties,
        dataSourceCreators: List<DataSourceCreator>,
    ): DefaultDataSourceCreator {
        val creator = DsDataSourceCreator()
        creator.setCreators(dataSourceCreators)
        creator.setPublicKey(properties.publicKey)
        creator.setLazy(properties.lazy)
        creator.setP6spy(properties.p6spy)
        creator.setSeata(properties.seata)
        creator.setSeataMode(properties.seataMode)
        return creator
    }

    /** 上下文路由解析器 bean。@Bean("dsContextProcessor") 显式指定名字，便于其它模块按名注入。 */
    @Bean("dsContextProcessor")
    @ConditionalOnMissingBean
    open fun dsContextProcessor() = DsContextProcessor()

    /** [IDynamicDataSourceLoad] 默认实现 —— 全 null。生产应用应自行实现并注册替换它。 */
    @Bean("dynamicDataSourceLoad")
    @ConditionalOnMissingBean
    open fun dynamicDataSourceLoad(): IDynamicDataSourceLoad = DefaultDynamicDataSourceLoad()

    /** kudos 组件初始化器名称，供 [IComponentInitializer] 的日志和顺序追踪使用。 */
    override fun getComponentName() = "kudos-ability-data-rdb-jdbc"

}
