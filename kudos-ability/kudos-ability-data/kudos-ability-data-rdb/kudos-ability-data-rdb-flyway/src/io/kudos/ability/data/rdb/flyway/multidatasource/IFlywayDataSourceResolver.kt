package io.kudos.ability.data.rdb.flyway.multidatasource

import javax.sql.DataSource

/**
 * SPI: resolves the `datasource-config` keys of this module into real [DataSource] instances.
 *
 * Purpose: decouple [FlywayMultiDataSourceMigrator] from any concrete multi-data-source
 * implementation. The default implementation ([DsContextFlywayDataSourceResolver]) delegates to
 * the baomidou dynamic-routing table; environments using a different data source mechanism can
 * register their own bean of this type ([io.kudos.ability.data.rdb.flyway.init.FlywayAutoConfiguration]
 * only registers the default when no other bean of this type exists).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IFlywayDataSourceResolver {

    /** Whether a data source is registered under [datasourceKey]. */
    fun hasDataSource(datasourceKey: String): Boolean

    /** Returns the data source registered under [datasourceKey], or `null` if none. */
    fun getDataSource(datasourceKey: String): DataSource?
}
