package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceSearchPayload
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
        get(id, SysDataSourceCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun listAllCacheItems(): List<SysDataSourceCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysDataSourceCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysDataSourceCacheItem>
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun listCacheItemsByIds(ids: Collection<String>): List<SysDataSourceCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysDataSourceCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysDataSourceCacheItem>
    }

    /**
     * 按租户id+子系统编码+微服务编码查询单条（tenantId 非空）。
     * 用于 Hash 缓存按二级索引查询。
     */
    open fun getDataSources(
        tenantId: String,
        subSystemCode: String?,
        microServiceCode: String?
    ): List<SysDataSourceCacheItem> {
        // 参数为 null/空时，查询条件为该列 IS NULL
        val nullProperties = mutableListOf<String>()
        if (subSystemCode.isNullOrBlank()) nullProperties.add(SysDataSource::subSystemCode.name)
        if (microServiceCode.isNullOrBlank()) nullProperties.add(SysDataSource::microServiceCode.name)
        val payload = SysDataSourceSearchPayload().apply {
            returnEntityClass = SysDataSourceCacheItem::class
            this.tenantId = tenantId
            this.subSystemCode = subSystemCode
            this.microServiceCode = microServiceCode
            operators = mapOf(SysDataSource::tenantId.name to OperatorEnum.IS_NOT_NULL)
            this.nullProperties = nullProperties
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysDataSourceCacheItem>
    }

}