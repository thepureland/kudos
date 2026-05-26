package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import io.kudos.base.logger.LogFactory

/**
 * Placeholder implementation of [IDynamicDataSourceLoad]: every dsId returns null
 * and a warn-level log is emitted.
 *
 * Serves as a fallback so projects that have not wired up dynamic data-source
 * loading can still start (routing works normally when hitting statically
 * configured data sources; when routing hits a dsId that "requires dynamic loading"
 * it fails, signaling to the developer that they need to implement a custom
 * [IDynamicDataSourceLoad]).
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DefaultDynamicDataSourceLoad : IDynamicDataSourceLoad {

    /** Default implementation: always returns null and emits a warn telling the framework user to implement [IDynamicDataSourceLoad]. */
    override fun getPropertyById(dsId: String?): DataSourceProperty? {
        log.warn("Default dynamic data source load returned null, {0}", dsId)
        return null
    }

    private val log = LogFactory.getLog(this::class)

}
