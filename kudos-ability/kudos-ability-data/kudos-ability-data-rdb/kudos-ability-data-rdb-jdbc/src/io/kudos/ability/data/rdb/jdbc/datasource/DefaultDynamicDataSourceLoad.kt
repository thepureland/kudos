package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import io.kudos.base.logger.LogFactory

/**
 * [IDynamicDataSourceLoad] 的占位实现：所有 dsId 都返回 null + 打 warn 日志。
 *
 * 起兜底作用，让"没接入动态数据源加载"的项目也能启动（路由命中静态配置的数据源时正常工作；
 * 命中"需要动态加载"的 dsId 时会 fail，开发者据此知道要实现自定义 [IDynamicDataSourceLoad]）。
 *
 * @author damon
 * @since 1.0.0
 */
class DefaultDynamicDataSourceLoad : IDynamicDataSourceLoad {

    /** 默认实现：始终返回 null，打 warn 提示框架使用方该实现 [IDynamicDataSourceLoad] 了。 */
    override fun getPropertyById(dsId: String?): DataSourceProperty? {
        log.warn("默认的动态数据源加载为空,{0}", dsId)
        return null
    }

    private val log = LogFactory.getLog(this::class)

}
