package io.kudos.ability.distributed.tx.seata.init

import com.baomidou.dynamic.datasource.enums.SeataMode
import io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.apache.seata.core.model.BranchType
import org.apache.seata.rm.datasource.DataSourceProxy
import org.apache.seata.rm.datasource.xa.DataSourceProxyXA
import org.springframework.beans.factory.annotation.Value
import javax.sql.DataSource

/**
 * Seata DataSource proxy implementation — wraps the upstream DataSource as Seata's
 * `DataSourceProxy` (AT) or `DataSourceProxyXA` (XA) based on `seata.data-source-proxy-mode`.
 *
 * AT and XA are mutually exclusive and map one-to-one to the yaml configuration. When
 * `enable-auto-data-source-proxy=false`, the original DataSource is returned as-is, which is
 * handy for bypassing Seata during integration debugging.
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
        if (!enableProxy) return dataSource
        return try {
            when {
                proxyMode.equals(BranchType.AT.name, ignoreCase = true) -> DataSourceProxy(dataSource)
                proxyMode.equals(BranchType.XA.name, ignoreCase = true) -> DataSourceProxyXA(dataSource)
                else -> throw IllegalArgumentException(
                    "Unrecognised seata.data-source-proxy-mode: '$proxyMode'. Valid values: ${BranchType.AT.name} | ${BranchType.XA.name} (case-insensitive)"
                )
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to proxy DataSource", e)
        }
    }

    override fun isSeata(): Boolean = true

    override fun seataMode(): SeataMode? = when {
        proxyMode.equals(BranchType.AT.name, ignoreCase = true) -> SeataMode.AT
        proxyMode.equals(BranchType.XA.name, ignoreCase = true) -> SeataMode.XA
        else -> null
    }
}
