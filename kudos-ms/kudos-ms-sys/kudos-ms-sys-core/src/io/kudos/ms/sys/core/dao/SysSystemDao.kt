package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.ms.sys.core.model.table.SysSystems
import org.springframework.stereotype.Repository


/**
 * 系统数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysSystemDao : BaseCrudDao<String, SysSystem, SysSystems>() {

    /** 按 code（主键）查询单条，返回缓存用 VO */
    open fun getCacheItem(code: String): SysSystemCacheItem? =
        get(code, SysSystemCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun listAllCacheItems(): List<SysSystemCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysSystemCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysSystemCacheItem>
    }

    /** 按 code 集合批量查询，返回缓存用 VO 列表 */
    open fun listCacheItemsByIds(codes: Collection<String>): List<SysSystemCacheItem> {
        if (codes.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysSystemCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, codes))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysSystemCacheItem>
    }

    /** 按是否子系统查询，返回缓存用 VO 列表 */
    open fun listCacheItemsBySubSystem(isSubSystem: Boolean): List<SysSystemCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysSystemCacheItem::class
            criterions = listOf(Criterion(SysSystem::subSystem.name, OperatorEnum.EQ, isSubSystem))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysSystemCacheItem>
    }
}