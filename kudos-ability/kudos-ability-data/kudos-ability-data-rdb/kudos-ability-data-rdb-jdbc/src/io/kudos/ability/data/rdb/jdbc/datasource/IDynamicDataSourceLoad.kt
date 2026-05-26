package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty

/**
 * Extension point for "loading a baomidou [DataSourceProperty] by dsId".
 *
 * When [DsContextProcessor.getDatasourceKey] finds that the current routing table
 * is missing the real DataSource for some dsId, it calls back here to request
 * "the connection info (url/user/pass/driver/...) for this dsId" so it can build
 * the data source on the fly and register it into the dynamic routing table.
 * Typical usage: pull the configuration by dsId from a configuration center /
 * metadata database.
 *
 * When not implemented, [DefaultDynamicDataSourceLoad] is used and all dsIds
 * return null — equivalent to "dynamic data-source loading is disabled"; routing
 * to an unconfigured entry will fail.
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDynamicDataSourceLoad {
    /**
     * Returns the baomidou [DataSourceProperty] for the given data source id; returns `null` when not found.
     */
    fun getPropertyById(dsId: String?): DataSourceProperty?
}
