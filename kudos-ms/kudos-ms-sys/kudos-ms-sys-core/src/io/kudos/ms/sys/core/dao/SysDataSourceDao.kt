package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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

    /** 按 id 查询单条，返回缓存用 VO */
    open fun getCacheItem(id: String): SysDataSourceCacheItem? =
        getAs(id, SysDataSourceCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun fetchAllDataSourcesForCache(): List<SysDataSourceCacheItem> {
        return search<SysDataSourceCacheItem>()
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun fetchDataSourcesByIdsForCache(ids: Collection<String>): List<SysDataSourceCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val criteria = Criteria.of(SysDataSource::id.name, OperatorEnum.IN, ids)
        return search(criteria, SysDataSourceCacheItem::class)
    }

    /**
     * 按租户id+子系统编码+微服务编码查询单条（tenantId 非空）。
     * 用于 Hash 缓存按二级索引查询。
     */
    open fun fetchDataSourcesForCache(
        tenantId: String,
        subSystemCode: String?,
        microServiceCode: String?
    ): List<SysDataSourceCacheItem> {
        val criteria = Criteria.of(SysDataSource::tenantId.name, OperatorEnum.EQ, tenantId)
        if (subSystemCode.isNullOrBlank()) {
            criteria.addAnd(SysDataSource::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
        }
        if (microServiceCode.isNullOrBlank()) {
            criteria.addAnd(SysDataSource::microServiceCode.name, OperatorEnum.EQ, subSystemCode)
        }
        return search<SysDataSourceCacheItem>(criteria)
    }

}