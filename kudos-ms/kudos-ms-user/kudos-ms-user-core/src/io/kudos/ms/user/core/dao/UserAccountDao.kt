package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
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

    /**
     * 按租户、用户名查询，返回缓存用 VO 列表
     *
     * @param tenantId 租户id
     * @param username 用户名
     * @return UserAccountCacheItem，不存在返回null
     */
    open fun getUsersByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheItem? {
        val criteria = Criteria.and(
            UserAccount::tenantId eq tenantId,
            UserAccount::username eq username
        )
        return searchAs<UserAccountCacheItem>(criteria).firstOrNull()
    }

    /**
     * 查询所有 active=true 的用户
     *
     * @return List<UserAccountCacheItem>
     */
    open fun searchActiveUsersForCache(): List<UserAccountCacheItem> {
        val criteria = Criteria(UserAccount::active eq true)
        return searchAs<UserAccountCacheItem>(criteria)
    }

    /**
     * 查询租户下所有启用用户ID
     *
     * @param tenantId 租户ID
     * @return 用户ID列表
     */
    fun searchActiveUserIdsByTenantId(tenantId: String): List<String> {
        val criteria = Criteria(UserAccount::tenantId eq tenantId)
            .addAnd(UserAccount::active eq true)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, UserAccount::id.name) as List<String>
    }

    //endregion your codes 2

}
