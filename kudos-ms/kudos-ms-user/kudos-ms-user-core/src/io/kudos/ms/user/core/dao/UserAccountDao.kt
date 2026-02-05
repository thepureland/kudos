package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.model.po.UserAccount
import io.kudos.ms.user.core.model.table.UserAccounts
import org.springframework.stereotype.Repository


/**
 * 用户数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserAccountDao : BaseCrudDao<String, UserAccount, UserAccounts>() {
//endregion your codes 1

    //region your codes 2

    /** 按 id（主键）查询单条，返回缓存用 VO */
    open fun getCacheItem(id: String): UserAccountCacheItem? =
        get(id, UserAccountCacheItem::class)

    /** 全量查询，返回缓存用 VO 列表（用于全量刷新） */
    open fun getAllUsersForCache(): List<UserAccountCacheItem> {
        val payload = ListSearchPayload().apply {
            returnEntityClass = UserAccountCacheItem::class
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<UserAccountCacheItem>
    }

    /** 按 id 集合批量查询，返回缓存用 VO 列表 */
    open fun getUsersByIdsForCache(ids: Collection<String>): List<UserAccountCacheItem> {
        if (ids.isEmpty()) return emptyList()
        val payload = ListSearchPayload().apply {
            returnEntityClass = UserAccountCacheItem::class
            criterions = listOf(Criterion("id", OperatorEnum.IN, ids))
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload) as List<UserAccountCacheItem>
    }

    /** 按租户、用户名查询，返回缓存用 VO 列表（用于按 tenantId+username 二级索引） */
    open fun getUsersByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheItem? {
        val payload = ListSearchPayload().apply {
            returnEntityClass = UserAccountCacheItem::class
            criterions = listOf(
                Criterion(UserAccount::tenantId.name, OperatorEnum.EQ, tenantId),
                Criterion(UserAccount::username.name, OperatorEnum.EQ, username)
            )
        }
        @Suppress("UNCHECKED_CAST")
        return search(payload).firstOrNull() as UserAccountCacheItem?
    }

    //endregion your codes 2

}
