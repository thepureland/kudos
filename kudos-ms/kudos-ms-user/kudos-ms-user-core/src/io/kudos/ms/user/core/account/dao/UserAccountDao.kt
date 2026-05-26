package io.kudos.ms.user.core.account.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.table.UserAccounts
import org.springframework.stereotype.Repository


/**
 * User account DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserAccountDao : BaseCrudDao<String, UserAccount, UserAccounts>() {


    /**
     * Query by tenant id + username, returning the cache VO.
     *
     * @param tenantId tenant id
     * @param username username
     * @return UserAccountCacheEntry, or null if not found
     */
    open fun getUsersByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry? {
        val criteria = Criteria.and(
            UserAccount::tenantId eq tenantId,
            UserAccount::username eq username
        )
        return searchAs<UserAccountCacheEntry>(criteria).firstOrNull()
    }

    /**
     * Query all users with active=true.
     *
     * @return List<UserAccountCacheEntry>
     */
    open fun searchActiveUsersForCache(): List<UserAccountCacheEntry> {
        val criteria = Criteria(UserAccount::active eq true)
        return searchAs<UserAccountCacheEntry>(criteria)
    }

    /**
     * Query all active user ids under the given tenant.
     *
     * @param tenantId tenant id
     * @return list of user ids
     */
    fun searchActiveUserIdsByTenantId(tenantId: String): List<String> {
        val criteria = Criteria(UserAccount::tenantId eq tenantId)
            .addAnd(UserAccount::active eq true)
        return searchProperty(criteria, UserAccount::id).filterNotNull()
    }


}
