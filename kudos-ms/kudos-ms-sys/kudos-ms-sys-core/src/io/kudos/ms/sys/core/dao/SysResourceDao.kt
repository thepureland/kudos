package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.ms.sys.core.model.table.SysResources
import org.springframework.stereotype.Repository

/**
 * 资源数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysResourceDao : BaseCrudDao<String, SysResource, SysResources>() {

    /** 按 id 查询单条，返回缓存用 VO */
    open fun getCacheItem(id: String): SysResourceCacheItem? =
        getAs(id, SysResourceCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun listAllCacheItems(): List<SysResourceCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysResourceCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysResourceCacheItem>
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun listCacheItemsByIds(ids: Collection<String>): List<SysResourceCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysResourceCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysResourceCacheItem>
    }

    /** 按子系统编码+URL+启用状态查询资源（url 非空） */
    open fun getResourceBySubSysAndUrl(subSystemCode: String, url: String, active: Boolean? = null): SysResourceCacheItem? {
        val criterions = mutableListOf(
            Criterion(SysResource::subSystemCode.name, OperatorEnum.EQ, subSystemCode),
            Criterion(SysResource::url.name, OperatorEnum.EQ, url),
        )
        if (active != null) {
            criterions.add(Criterion(SysResource::active.name, OperatorEnum.EQ, active))
        }
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysResourceCacheItem::class
            this.criterions = criterions
        }
        return search(payload).firstOrNull() as SysResourceCacheItem?
    }

    /** 按子系统编码+资源类型代码查询资源 id 列表 */
    @Suppress("UNCHECKED_CAST")
    open fun getResourceIdsBySubSysAndType(subSystemCode: String, resourceTypeDictCode: String): List<String> {
        val criteria = Criteria.of(SysResource::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
            .addAnd(SysResource::resourceTypeDictCode.name, OperatorEnum.EQ, resourceTypeDictCode)
        return searchProperty(criteria, SysResource::id.name) as List<String>
    }
}