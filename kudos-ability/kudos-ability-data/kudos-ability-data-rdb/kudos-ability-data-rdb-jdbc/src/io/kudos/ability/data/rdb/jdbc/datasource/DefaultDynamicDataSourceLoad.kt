package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import org.soul.base.log.Log
import org.soul.base.log.LogFactory

/**
 * 默认动态数据源加载
 *
 * @author damon
 */
class DefaultDynamicDataSourceLoad : IDynamicDataSourceLoad {
    override fun getPropertyById(dsId: Int?): DataSourceProperty? {
        log.warn("默认的动态数据源加载为空,{0}", dsId)
        return null
    }

    companion object {
        private val log: Log = LogFactory.getLog(DefaultDynamicDataSourceLoad::class.java)
    }
}
