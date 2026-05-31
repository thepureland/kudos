package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.model.table.AuthRoleUsers
import org.springframework.stereotype.Repository
import java.time.LocalDateTime


/**
 * Role-user relation DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class AuthRoleUserDao : BaseCrudDao<String, AuthRoleUser, AuthRoleUsers>() {


    /**
     * Checks whether the relation exists.
     *
     * @param roleId role id
     * @param userId user id
     * @return true if it exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean {
        val criteria = Criteria.and(
            AuthRoleUser::roleId eq roleId,
            AuthRoleUser::userId eq userId
        )
        return count(criteria) > 0
    }

    /**
     * Queries role IDs **currently active** for a user. A temporal grant (with start_time/end_time)
     * counts only while [now] falls inside its window; permanent grants (both NULL) always count.
     * This is the permission-resolution contract — the caches that drive access checks call it.
     *
     * @param userId user id
     * @param now the instant to evaluate the validity window against (defaults to current time)
     * @return List<role id> for grants active at [now]
     */
    fun searchRoleIdsByUserId(userId: String, now: LocalDateTime = LocalDateTime.now()): List<String> {
        val criteria = Criteria(AuthRoleUser::userId eq userId)
        return search(criteria).filter { isActiveAt(it, now) }.map { it.roleId }
    }

    /**
     * Returns all **currently active** role-user relations grouped by user id as
     * "user id -> list of role ids". Out-of-window temporal grants are excluded; permanent grants
     * (start_time/end_time both NULL) are always included.
     *
     * @param now the instant to evaluate the validity windows against (defaults to current time)
     * @return Map<user id, List<role id>>
     */
    fun searchAllUserIdToRoleIdsForCache(now: LocalDateTime = LocalDateTime.now()): Map<String, List<String>> {
        val all = allSearch().filter { isActiveAt(it, now) }
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.roleId } }
    }

    /**
     * Returns grants whose validity window has already ended (end_time < [now]). Used by the purge
     * sweep. NULL end_time (never-expires) rows are excluded by the comparison. Indexed on end_time.
     *
     * @param now the instant to compare expiry against (defaults to current time)
     * @return the expired grant rows (carry id / roleId / userId for deletion + cache eviction)
     */
    open fun searchExpiredGrants(now: LocalDateTime = LocalDateTime.now()): List<AuthRoleUser> {
        val criteria = Criteria(AuthRoleUser::endTime.name, OperatorEnum.LT, now)
        return search(criteria)
    }

    /** A grant is active at [now] when now is within [start, end] (NULL bounds are open). */
    private fun isActiveAt(grant: AuthRoleUser, now: LocalDateTime): Boolean {
        val start = grant.startTime
        val end = grant.endTime
        return (start == null || !start.isAfter(now)) && (end == null || !end.isBefore(now))
    }

    /**
     * Queries user IDs by role id.
     *
     * @param roleId role id
     * @return List<user id>
     */
    fun searchUserIdsByRoleId(roleId: String): List<String> {
        val criteria = Criteria(AuthRoleUser::roleId eq roleId)
        return searchProperty(criteria, AuthRoleUser::userId).filterNotNull()
    }

    /**
     * Returns all raw auth_role_user rows for [roleId], including time-window fields
     * (startTime / endTime). Used by the temporal-grant admin view to show who holds this
     * role and when each grant expires.
     *
     * @param roleId role id
     * @return full grant rows (all windows, including past and future)
     */
    open fun searchGrantsByRoleId(roleId: String): List<AuthRoleUser> {
        val criteria = Criteria(AuthRoleUser::roleId eq roleId)
        return search(criteria)
    }

    /**
     * Returns all role-user relations grouped by role id as "role id -> list of user ids".
     *
     * @return Map<role id, List<user id>>
     */
    fun getAllRoleIdToUserIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * Deletes a relation by role id and user id.
     *
     * @param roleId role id
     * @param userId user id
     * @return number of rows deleted
     */
    fun deleteByRoleIdAndUserId(roleId: String, userId: String): Int {
        val criteria = Criteria.and(
            AuthRoleUser::roleId eq roleId,
            AuthRoleUser::userId eq userId
        )
        return batchDeleteCriteria(criteria)
    }


}
