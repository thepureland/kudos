package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty

/**
 * 动态数据源加载
 *
 * @author damon
 */
interface IDynamicDataSourceLoad {
    /**
     * 根据dsId获取数据源配置
     */
    fun getPropertyById(dsId: String?): DataSourceProperty?
}
