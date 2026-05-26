package io.kudos.ms.sys.core.datasource.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource
import io.kudos.ms.sys.core.datasource.model.table.SysDataSources
import org.springframework.stereotype.Repository


/**
 * Data source DAO.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysDataSourceDao : BaseCrudDao<String, SysDataSource, SysDataSources>() {

    /** Batch query by id collection, returning cache VO list. */
    open fun fetchDataSourcesByIdsForCache(ids: Collection<String>): List<SysDataSourceCacheEntry> {
        if (ids.isEmpty()) return emptyList()
        val criteria = Criteria(SysDataSource::id inList ids)
        return searchAs<SysDataSourceCacheEntry>(criteria)
    }

    /**
     * Query records by tenant id + sub-system code + micro-service code (tenantId is required).
     *
     * @param tenantId tenant id
     * @param subSystemCode sub-system code; blank value is ignored as a condition
     * @param microServiceCode micro-service code; blank value is ignored as a condition
     * @return List<SysDataSourceCacheEntry>
     */
    open fun fetchDataSourcesForCache(
        tenantId: String,
        subSystemCode: String?,
        microServiceCode: String?
    ): List<SysDataSourceCacheEntry> {
        val criteria = Criteria(SysDataSource::tenantId eq tenantId)
        if (!subSystemCode.isNullOrBlank()) {
            criteria.addAnd(SysDataSource::subSystemCode eq subSystemCode)
        }
        if (!microServiceCode.isNullOrBlank()) {
            criteria.addAnd(SysDataSource::microServiceCode eq microServiceCode)
        }
        return searchAs<SysDataSourceCacheEntry>(criteria)
    }

}