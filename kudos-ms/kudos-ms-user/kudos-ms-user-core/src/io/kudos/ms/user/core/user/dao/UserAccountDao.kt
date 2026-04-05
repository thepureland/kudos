package io.kudos.ms.user.core.user.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.common.user.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.user.model.po.UserAccount
import io.kudos.ms.user.core.user.model.table.UserAccounts
import org.springframework.stereotype.Repository


/**
 * 用户数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserAccountDao : BaseCrudDao<String, UserAccount, UserAccounts>() {


    /**
     * 按租户、用户名查询，返回缓存用 VO 列表
     *
     * @param tenantId 租户id
     * @param username 用户名
     * @return UserAccountCacheEntry，不存在返回null
     */
    open fun getUsersByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry? {
        val criteria = Criteria.and(
            UserAccount::tenantId eq tenantId,
            UserAccount::username eq username
        )
        return searchAs<UserAccountCacheEntry>(criteria).firstOrNull()
    }

    /**
     * 查询所有 active=true 的用户
     *
     * @return List<UserAccountCacheEntry>
     */
    open fun searchActiveUsersForCache(): List<UserAccountCacheEntry> {
        val criteria = Criteria(UserAccount::active eq true)
        return searchAs<UserAccountCacheEntry>(criteria)
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
        return searchProperty(criteria, UserAccount::id).filterNotNull()
    }


}
