package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.enums.SeataMode
import javax.sql.DataSource

/**
 * Extension point for data source proxying.
 *
 * After [DsDataSourceCreator] creates each dynamic data source, [proxyDatasource]
 * is called to let an external component "wrap" its own proxy (typical use:
 * Seata's `DataSourceProxy` intercepts commit/rollback to implement AT mode).
 *
 * - When not implemented (no bean in the container): returns the original
 *   datasource, equivalent to no proxy.
 * - [isSeata] / [seataMode] signals are used internally by baomidou to set the
 *   ItemDataSource flags.
 *
 * Currently only the Seata module (kudos-ability-distributed-tx-seata) registers
 * a `SeataDataSourceProxy` instance into the container; other projects can
 * register their own proxies (monitoring / encryption / auditing, etc.).
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDataSourceProxy {
    /**
     * Proxies / wraps a data source. The default implementation returns the original
     * data source directly ("no proxy").
     */
    fun proxyDatasource(dataSource: DataSource): DataSource {
        return dataSource
    }

    /** Whether Seata integration is enabled; baomidou's `ItemDataSource` sets a flag accordingly. */
    fun isSeata(): Boolean = false

    /** Seata mode (AT / XA / TCC / SAGA); only meaningful when [isSeata] is true. */
    fun seataMode(): SeataMode? {
        return null
    }
}
