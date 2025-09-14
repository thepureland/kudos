package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import io.kudos.base.logger.LogFactory

/**
 * 默认动态数据源加载
 *
 * @author damon
 */
class DefaultDynamicDataSourceLoad : IDynamicDataSourceLoad {

    override fun getPropertyById(dsId: String?): DataSourceProperty? {
        log.warn("默认的动态数据源加载为空,{0}", dsId)
        return null
    }

    private val log = LogFactory.getLog(this)

}
