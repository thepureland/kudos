package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.core.model.po.SysDataSource
import io.kudos.ms.sys.core.model.table.SysDataSources
import org.springframework.stereotype.Repository


/**
 * 数据源数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDataSourceDao : BaseCrudDao<String, SysDataSource, SysDataSources>() {
//endregion your codes 1

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun fetchDataSourcesByIdsForCache(ids: Collection<String>): List<SysDataSourceCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val criteria = Criteria(SysDataSource::id inList ids)
        return searchAs<SysDataSourceCacheItem>(criteria)
    }

    /**
     * 按租户id+子系统编码+微服务编码查询单条（tenantId 非空）。
     *
     * @param tenantId 租户ID
     * @param subSystemCode 子系统编号，为空将不作为查询条件
     * @param microServiceCode 微服务编号，为空将不作为查询条件
     * @return List<SysDataSourceCacheItem>
     */
    open fun fetchDataSourcesForCache(
        tenantId: String,
        subSystemCode: String?,
        microServiceCode: String?
    ): List<SysDataSourceCacheItem> {
        val criteria = Criteria(SysDataSource::tenantId eq tenantId)
        if (!subSystemCode.isNullOrBlank()) {
            criteria.addAnd(SysDataSource::subSystemCode eq subSystemCode)
        }
        if (!microServiceCode.isNullOrBlank()) {
            criteria.addAnd(SysDataSource::microServiceCode eq microServiceCode)
        }
        return searchAs<SysDataSourceCacheItem>(criteria)
    }

}