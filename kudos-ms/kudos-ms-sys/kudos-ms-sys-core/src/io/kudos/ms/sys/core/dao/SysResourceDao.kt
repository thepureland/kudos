package io.kudos.ms.sys.core.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.ms.sys.core.model.table.SysResources
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao

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
        get(id, SysResourceCacheItem::class)

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
            criterions = listOf(io.kudos.base.query.Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysResourceCacheItem>
    }

    /** 按子系统编码+URL 查询资源 id（仅 url 非空且 active=true），兼容旧调用 */
    open fun getResourceIdBySubSysAndUrl(subSystemCode: String, url: String): String? =
        getResourceIdBySubSysAndUrl(subSystemCode, url, true)

    /** 按子系统编码+URL+启用状态查询资源 id（url 非空） */
    open fun getResourceIdBySubSysAndUrl(subSystemCode: String, url: String, active: Boolean): String? {
        val criteria = Criteria.of(SysResource::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
            .addAnd(SysResource::url.name, OperatorEnum.EQ, url)
            .addAnd(SysResource::active.name, OperatorEnum.EQ, active)
        val ids = searchProperty(criteria, SysResource::id.name)
        return ids.firstOrNull() as? String
    }

    /** 按子系统编码+资源类型代码查询资源 id 列表（仅 active=true） */
    @Suppress("UNCHECKED_CAST")
    open fun getResourceIdsBySubSysAndType(subSystemCode: String, resourceTypeDictCode: String): List<String> {
        val criteria = Criteria.of(SysResource::active.name, OperatorEnum.EQ, true)
            .addAnd(SysResource::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
            .addAnd(SysResource::resourceTypeDictCode.name, OperatorEnum.EQ, resourceTypeDictCode)
        return searchProperty(criteria, SysResource::id.name) as List<String>
    }
}