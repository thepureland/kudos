package io.kudos.ms.sys.core.dao

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.ms.sys.core.model.table.SysMicroServices
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 微服务数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysMicroServiceDao : BaseCrudDao<String, SysMicroService, SysMicroServices>() {
//endregion your codes 1

    /** 按 code（主键）查询单条，返回缓存用 VO */
    open fun getCacheItem(code: String): SysMicroServiceCacheItem? =
        get(code, SysMicroServiceCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun listAllCacheItems(): List<SysMicroServiceCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysMicroServiceCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysMicroServiceCacheItem>
    }

    /** 按 code 集合批量查询，返回缓存用 VO 列表 */
    open fun listCacheItemsByIds(codes: Collection<String>): List<SysMicroServiceCacheItem> {
        if (codes.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysMicroServiceCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, codes))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysMicroServiceCacheItem>
    }

    /** 按是否为原子服务查询，返回缓存用 VO 列表 */
    open fun listCacheItemsByAtomicService(atomicService: Boolean): List<SysMicroServiceCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = SysMicroServiceCacheItem::class
            criterions = listOf(Criterion(SysMicroService::atomicService.name, OperatorEnum.EQ, atomicService))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<SysMicroServiceCacheItem>
    }
}