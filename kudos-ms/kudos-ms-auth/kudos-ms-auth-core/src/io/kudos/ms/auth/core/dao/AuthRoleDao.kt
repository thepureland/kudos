package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.ms.auth.core.model.table.AuthRoles
import org.springframework.stereotype.Repository


/**
 * 角色数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleDao : BaseCrudDao<String, AuthRole, AuthRoles>() {
//endregion your codes 1

    //region your codes 2

    /** 按 id（主键）查询单条，返回缓存用 VO */
    open fun getCacheItem(id: String): AuthRoleCacheItem? =
        get(id, AuthRoleCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun getAllRolesForCache(): List<AuthRoleCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthRoleCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<AuthRoleCacheItem>
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun getRolesByIdsForCache(ids: Collection<String>): List<AuthRoleCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthRoleCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<AuthRoleCacheItem>
    }

    /** 查询所有 active=true 的角色（供 UserIdsByRoleIdCache / UserIdsByTenantIdAndRoleCodeCache.reloadAll） */
    open fun getActiveRolesForCache(): List<AuthRoleCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthRoleCacheItem::class
            criterions = listOf(Criterion(AuthRole::active.name, OperatorEnum.EQ, true))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<AuthRoleCacheItem>
    }

    /** 按租户、角色编码查询（不区分 active），返回单条缓存用 VO */
    open fun getRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheItem? {
        val payload = ListSearchPayload().apply {
            returnEntityClass = AuthRoleCacheItem::class
            criterions = listOf(
                Criterion(AuthRole::tenantId.name, OperatorEnum.EQ, tenantId),
                Criterion(AuthRole::code.name, OperatorEnum.EQ, code)
            )
        }
        @Suppress("UNCHECKED_CAST")
        return (search(payload) as List<AuthRoleCacheItem>).firstOrNull()
    }

    //endregion your codes 2

}
