package io.kudos.ability.distributed.tx.seata.init

import com.baomidou.dynamic.datasource.enums.SeataMode
import io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.apache.seata.core.model.BranchType
import org.apache.seata.rm.datasource.DataSourceProxy
import org.apache.seata.rm.datasource.xa.DataSourceProxyXA
import org.springframework.beans.factory.annotation.Value
import javax.sql.DataSource

/**
 * Seata数据源代理,支持AT/XA
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class SeataDataSourceProxy : IDataSourceProxy {

    @Value($$"${seata.data-source-proxy-mode}")
    private val proxyMode: String? = null

    @Value($$"${seata.enable-auto-data-source-proxy}")
    private var enableProxy: Boolean = true

    override fun proxyDatasource(dataSource: DataSource): DataSource {
        if (!enableProxy) {
            return dataSource
        }
        try {
            if (BranchType.AT.name.equals(proxyMode, ignoreCase = true)) {
                return DataSourceProxy(dataSource)
            }
            if (BranchType.XA.name.equals(proxyMode, ignoreCase = true)) {
                return DataSourceProxyXA(dataSource)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("代理数据源失败", e)
        }
        throw IllegalArgumentException("Unknown dataSourceProxyMode: $proxyMode")
    }

    override fun isSeata(): Boolean {
        return true
    }

    override fun seataMode(): SeataMode? {
        if (BranchType.AT.name.equals(proxyMode, ignoreCase = true)) {
            return SeataMode.AT
        }
        if (BranchType.XA.name.equals(proxyMode, ignoreCase = true)) {
            return SeataMode.XA
        }
        return null
    }
}
