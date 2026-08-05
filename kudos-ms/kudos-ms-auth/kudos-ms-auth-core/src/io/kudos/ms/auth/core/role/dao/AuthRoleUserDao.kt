package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
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
 * @author AI: Claude
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
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean {
        val criteria = Criteria.and(
            AuthRoleUser::roleId eq roleId,
            AuthRoleUser::userId eq userId
        )
        return search(criteria).any { it.isLive() }
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
        return search(criteria).filter { it.isLive() && isActiveAt(it, now) }.map { it.roleId }
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
        val all = allSearch().filter { it.isLive() && isActiveAt(it, now) }
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

    /**
     * Returns grants whose `start_time` falls inside `(since, now]` — the ones that just became
     * effective. NULL start_time (effective immediately) rows never appear: they were already live
     * when written, so their caches were computed correctly at that point.
     *
     * @param since exclusive lower bound
     * @param now inclusive upper bound
     * @return the grant rows that crossed into their window during the interval
     */
    open fun searchGrantsStartedBetween(since: LocalDateTime, now: LocalDateTime): List<AuthRoleUser> {
        val criteria = Criteria.and(
            Criteria(AuthRoleUser::startTime.name, OperatorEnum.GT, since),
            Criteria(AuthRoleUser::startTime.name, OperatorEnum.LE, now),
        )
        return search(criteria)
    }

    /**
     * A revoked grant is gone as far as permission resolution is concerned; the row survives only so
     * the delegation chain stays walkable and the audit trail stays complete. Every read path that
     * answers "what can this principal do" must go through this.
     */
    private fun AuthRoleUser.isLive(): Boolean = revoked != true

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
        return search(criteria).filter { it.isLive() }.map { it.userId }
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
        val all = allSearch().filter { it.isLive() }
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * The live grant through which [userId] holds [roleId] **directly**.
     *
     * Deliberately excludes roles reached through a group or through the parent chain: delegation
     * authority travels only along the direct-grant chain, because that is the only path where
     * "who handed this to you" resolves to a person rather than to a membership.
     *
     * @param roleId the role
     * @param userId the holder
     * @param now the instant to evaluate the validity window against
     * @return the grant row, or null when the principal does not hold the role directly
     */
    open fun findLiveDirectGrant(
        roleId: String,
        userId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): AuthRoleUser? {
        val criteria = Criteria.and(
            AuthRoleUser::roleId eq roleId,
            AuthRoleUser::userId eq userId,
        )
        return search(criteria).firstOrNull { it.isLive() && isActiveAt(it, now) }
    }

    /**
     * Every grant descended from [grantIds] through `parent_grant_id`, excluding the roots.
     *
     * Walks generation by generation with a depth cap, mirroring the parent-chain walks on the role
     * tree: the column is a plain self-reference with no database-level cycle guard, and a
     * revocation sweep must never be the thing that spins.
     *
     * @param grantIds the roots to descend from
     * @param maxDepth safety cap on the walk
     * @return the descendant grants, deduplicated
     */
    open fun searchDelegatedDescendants(
        grantIds: Collection<String>,
        maxDepth: Int = 32,
    ): List<AuthRoleUser> {
        if (grantIds.isEmpty()) return emptyList()
        val seen = HashSet(grantIds)
        val descendants = mutableListOf<AuthRoleUser>()
        var frontier: Set<String> = grantIds.toSet()
        var depth = 0
        while (frontier.isNotEmpty() && depth < maxDepth) {
            val children = search(Criteria(AuthRoleUser::parentGrantId inList frontier.toList()))
                .filter { seen.add(it.id) }
            if (children.isEmpty()) break
            descendants += children
            frontier = children.map { it.id }.toSet()
            depth++
        }
        return descendants
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
