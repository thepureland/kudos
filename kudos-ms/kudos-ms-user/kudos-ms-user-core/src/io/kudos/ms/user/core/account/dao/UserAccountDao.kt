package io.kudos.ms.user.core.account.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.table.UserAccounts
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNull
import org.ktorm.dsl.update
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime


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

    /** Takes a real writer lock for account-scoped read-then-write invariants. */
    open fun lockAccount(userId: String) {
        require(userId.isNotBlank()) { "userId must not be blank." }
        val touched = database().useConnection { connection ->
            connection.prepareStatement(
                """update "user_account" set "update_time" = ? where "id" = ?"""
            ).use { statement ->
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                statement.setString(2, userId)
                statement.executeUpdate()
            }
        }
        check(touched > 0) { "User account $userId no longer exists; the write is refused." }
    }

    /** Compare-and-set password encoding so a concurrent password change can never be overwritten. */
    open fun upgradeLoginPasswordEncoding(
        id: String,
        expectedEncodedPassword: String,
        upgradedEncodedPassword: String,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "user_account" set "login_password" = ?, "update_time" = ? """ +
                """where "id" = ? and "login_password" = ?"""
        ).use { statement ->
            statement.setString(1, upgradedEncodedPassword)
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
            statement.setString(3, id)
            statement.setString(4, expectedEncodedPassword)
            statement.executeUpdate() == 1
        }
    }

    /** Activates first-time TOTP enrollment without overwriting a concurrently installed authenticator. */
    open fun activateAuthenticationKeyIfAbsent(id: String, secret: String): Boolean =
        database().update(UserAccounts) {
            set(UserAccounts.authenticationKey, secret)
            set(UserAccounts.updateTime, LocalDateTime.now())
            where {
                (UserAccounts.id eq id) and UserAccounts.authenticationKey.isNull()
            }
        } == 1


}
