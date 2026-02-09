package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.core.model.po.UserOrg
import io.kudos.ms.user.core.model.table.UserOrgs
import org.springframework.stereotype.Repository


/**
 * 机构数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserOrgDao : BaseCrudDao<String, UserOrg, UserOrgs>() {
//endregion your codes 1

    //region your codes 2

    /** 按 id（主键）查询单条，返回缓存用 VO */
    open fun getCacheItem(id: String): UserOrgCacheItem? =
        get(id, UserOrgCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun getAllOrgsForCache(): List<UserOrgCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = UserOrgCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<UserOrgCacheItem>
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun getOrgsByIdsForCache(ids: Collection<String>): List<UserOrgCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = UserOrgCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<UserOrgCacheItem>
    }

    /** 按租户ID查询，返回缓存用 VO 列表 */
    open fun getOrgsByTenantIdForCache(tenantId: String): List<UserOrgCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = UserOrgCacheItem::class
            criterions = listOf(
                Criterion(UserOrg::tenantId.name, OperatorEnum.EQ, tenantId),
            )
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<UserOrgCacheItem>
    }

    //endregion your codes 2

}
