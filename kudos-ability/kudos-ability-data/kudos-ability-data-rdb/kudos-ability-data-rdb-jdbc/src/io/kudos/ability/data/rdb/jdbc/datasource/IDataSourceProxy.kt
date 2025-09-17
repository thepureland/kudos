package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.enums.SeataMode
import javax.sql.DataSource

/**
 * 数据源代理
 *
 * @author damon
 */
interface IDataSourceProxy {
    /**
     * 代理数据源
     *
     * @param dataSource
     */
    fun proxyDatasource(dataSource: DataSource): DataSource {
        return dataSource
    }

    fun isSeata(): Boolean = false

    fun seataMode(): SeataMode? {
        return null
    }
}
