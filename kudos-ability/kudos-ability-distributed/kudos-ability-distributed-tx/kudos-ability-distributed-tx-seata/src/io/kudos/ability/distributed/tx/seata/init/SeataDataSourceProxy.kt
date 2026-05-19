package io.kudos.ability.distributed.tx.seata.init

import com.baomidou.dynamic.datasource.enums.SeataMode
import io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceProxy
import org.apache.seata.core.model.BranchType
import org.apache.seata.rm.datasource.DataSourceProxy
import org.apache.seata.rm.datasource.xa.DataSourceProxyXA
import org.springframework.beans.factory.annotation.Value
import javax.sql.DataSource

/**
 * Seata 数据源代理实现——按 `seata.data-source-proxy-mode` 把上游 DataSource 包成
 * Seata `DataSourceProxy`(AT) 或 `DataSourceProxyXA`(XA)。
 *
 * AT 与 XA 二选一，与 yaml 配置一一对应。`enable-auto-data-source-proxy=false` 时
 * 直接返回原 DataSource，方便联调时绕过 Seata。
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
                    "未识别的 seata.data-source-proxy-mode: '$proxyMode'。合法值: ${BranchType.AT.name} | ${BranchType.XA.name}（不区分大小写）"
                )
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("代理数据源失败", e)
        }
    }

    override fun isSeata(): Boolean = true

    override fun seataMode(): SeataMode? = when {
        proxyMode.equals(BranchType.AT.name, ignoreCase = true) -> SeataMode.AT
        proxyMode.equals(BranchType.XA.name, ignoreCase = true) -> SeataMode.XA
        else -> null
    }
}
