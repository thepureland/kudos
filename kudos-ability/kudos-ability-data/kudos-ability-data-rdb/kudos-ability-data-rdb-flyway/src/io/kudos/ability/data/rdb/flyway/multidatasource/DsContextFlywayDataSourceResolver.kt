package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import jakarta.annotation.Resource
import javax.sql.DataSource

/**
 * Default [IFlywayDataSourceResolver]: delegates to [DsContextProcessor], i.e. the dynamic-routing
 * table provided by the baomidou dynamic-datasource starter (in single-data-source setups
 * DsContextProcessor degrades to "always the only data source").
 *
 * Replace by registering another [IFlywayDataSourceResolver] bean — this default backs off via
 * `@ConditionalOnMissingBean` in [io.kudos.ability.data.rdb.flyway.init.FlywayAutoConfiguration].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class DsContextFlywayDataSourceResolver : IFlywayDataSourceResolver {

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    override fun hasDataSource(datasourceKey: String): Boolean =
        dsContextProcessor.haveDataSource(datasourceKey)

    override fun getDataSource(datasourceKey: String): DataSource? =
        dsContextProcessor.getDataSource(datasourceKey)
}
