package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.auth.common.vo.group.AuthGroupCacheItem
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.ms.auth.core.model.table.AuthGroups
import org.springframework.stereotype.Repository


/**
 * 用户组数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthGroupDao : BaseCrudDao<String, AuthGroup, AuthGroups>() {
//endregion your codes 1

    //region your codes 2

    /** 按 id（主键）查询单条，返回缓存用 VO */
    open fun getCacheItem(id: String): AuthGroupCacheItem? =
        get(id, AuthGroupCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun getAllGroupsForCache(): List<AuthGroupCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthGroupCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<AuthGroupCacheItem>
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun getGroupsByIdsForCache(ids: Collection<String>): List<AuthGroupCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthGroupCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<AuthGroupCacheItem>
    }

    /** 按租户、用户组编码查询（不区分 active），返回单条缓存用 VO */
    open fun getGroupByTenantIdAndGroupCode(tenantId: String, code: String): AuthGroupCacheItem? {
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthGroupCacheItem::class
            criterions = listOf(
                Criterion(AuthGroup::tenantId.name, OperatorEnum.EQ, tenantId),
                Criterion(AuthGroup::code.name, OperatorEnum.EQ, code)
            )
        }
        @Suppress("UNCHECKED_CAST")
        return (search(payload) as List<AuthGroupCacheItem>).firstOrNull()
    }

    //endregion your codes 2

}
