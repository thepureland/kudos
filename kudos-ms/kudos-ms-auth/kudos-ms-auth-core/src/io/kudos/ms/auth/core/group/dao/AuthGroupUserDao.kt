package io.kudos.ms.auth.core.group.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.model.table.AuthGroupUsers
import org.springframework.stereotype.Repository
import java.time.LocalDateTime


/**
 * Group-user relation DAO.
 *
 * Two families of read method live here, and picking the wrong one is a security bug in one
 * direction and a staleness bug in the other:
 *
 * - **`search*` (filtered)** — the permission contract. A membership counts only while it is
 *   un-revoked and inside its validity window, because joining a group *is* being granted every
 *   role the group carries. Everything that answers "what can this principal do" goes through these.
 * - **[searchMemberUserIdsByGroupId] / [searchMembershipsByGroupId] (raw)** — every row regardless
 *   of state. Cache **invalidation** must use these: evicting a user who turned out not to need it
 *   is free, whereas missing one leaves a stale grant cached with nothing left to evict it.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthGroupUserDao : BaseCrudDao<String, AuthGroupUser, AuthGroupUsers>() {


    /**
     * Checks whether a membership **row** exists for the pair — regardless of whether it is
     * currently in force. This is row-level existence, used for write-side replace/dedup decisions;
     * for "is this principal a member right now", use [searchUserIdsByGroupId].
     *
     * @param groupId group ID
     * @param userId user ID
     * @return true if a row exists
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun exists(groupId: String, userId: String): Boolean {
        val criteria = Criteria.and(
            AuthGroupUser::groupId eq groupId,
            AuthGroupUser::userId eq userId
        )
        return count(criteria) > 0
    }

    /**
     * The user IDs whose membership of [groupId] is **in force** at [now]. Revoked memberships and
     * those outside their window are excluded; memberships with both bounds NULL always count.
     *
     * @param groupId group ID
     * @param now the instant to evaluate the validity window against (defaults to current time)
     * @return set of user IDs holding a live membership
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun searchUserIdsByGroupId(groupId: String, now: LocalDateTime = LocalDateTime.now()): Set<String> {
        val criteria = Criteria(AuthGroupUser::groupId eq groupId)
        return search(criteria).filter { it.isLive() && isActiveAt(it, now) }.map { it.userId }.toSet()
    }

    /**
     * Every user ID with a membership row in [groupId], **in force or not**.
     *
     * Cache invalidation calls this rather than [searchUserIdsByGroupId]: when a group's roles
     * change, the set that must be evicted includes the future-dated member (whose cache will be
     * built from the new roles when their window opens) and the just-expired one (whose cache still
     * holds the old roles). Filtering here would leave exactly those entries stale.
     *
     * @param groupId group ID
     * @return set of user IDs with any membership row
     */
    open fun searchMemberUserIdsByGroupId(groupId: String): Set<String> {
        val criteria = Criteria(AuthGroupUser::groupId eq groupId)
        return searchProperty(criteria, AuthGroupUser::userId).toSet()
    }

    /**
     * All membership rows of a group, windows included — the admin view, which must show memberships
     * that are not currently in force as well as those that are.
     *
     * @param groupId group ID
     * @return every membership row of the group
     */
    open fun searchMembershipsByGroupId(groupId: String): List<AuthGroupUser> =
        search(Criteria(AuthGroupUser::groupId eq groupId))

    /**
     * The groups the user belongs to **right now** — revoked and out-of-window memberships excluded.
     * This is the permission-resolution contract: the caches that drive access checks call it, so a
     * lapsed membership must stop contributing the group's roles here and nowhere else.
     *
     * @param userId user ID
     * @param now the instant to evaluate the validity window against (defaults to current time)
     * @return set of group IDs whose membership is in force
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun searchGroupIdsByUserId(userId: String, now: LocalDateTime = LocalDateTime.now()): Set<String> {
        val criteria = Criteria(AuthGroupUser::userId eq userId)
        return search(criteria).filter { it.isLive() && isActiveAt(it, now) }.map { it.groupId }.toSet()
    }

    /**
     * Every **in-force** membership grouped by user ID into a "userId -> groupIds" map.
     *
     * @param now the instant to evaluate the validity windows against (defaults to current time)
     * @return map of user id to its list of group ids
     */
    fun searchAllUserIdToGroupIdsForCache(now: LocalDateTime = LocalDateTime.now()): Map<String, List<String>> {
        val all = allSearch().filter { it.isLive() && isActiveAt(it, now) }
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.groupId } }
    }

    /**
     * Every **in-force** membership grouped by group ID into a "groupId -> userIds" map.
     *
     * @param now the instant to evaluate the validity windows against (defaults to current time)
     * @return map of group id to its list of user ids
     */
    fun searchAllGroupIdToUserIdsForCache(now: LocalDateTime = LocalDateTime.now()): Map<String, List<String>> {
        val all = allSearch().filter { it.isLive() && isActiveAt(it, now) }
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * Memberships that crossed **either** edge of their validity window inside `(since, now]` — the
     * ones whose `start_time` just arrived, and the ones whose `end_time` just passed.
     *
     * Both edges are windowed rather than absolute (`end_time < now` would match every membership
     * that ever expired). The sweep's only job is to evict caches, so re-announcing rows that
     * lapsed last year every minute would be pure waste — an interval query announces each boundary
     * crossing exactly once. Rows with NULL bounds never appear: they were correct when written.
     *
     * @param since exclusive lower bound
     * @param now inclusive upper bound
     * @return the membership rows whose window opened or closed during the interval
     */
    open fun searchMembershipsCrossingWindowBoundary(
        since: LocalDateTime,
        now: LocalDateTime,
    ): List<AuthGroupUser> {
        val started = Criteria.and(
            Criteria(AuthGroupUser::startTime.name, OperatorEnum.GT, since),
            Criteria(AuthGroupUser::startTime.name, OperatorEnum.LE, now),
        )
        val ended = Criteria.and(
            Criteria(AuthGroupUser::endTime.name, OperatorEnum.GT, since),
            Criteria(AuthGroupUser::endTime.name, OperatorEnum.LE, now),
        )
        // Two queries rather than one OR: `Criteria.or` of two composite AND branches is exactly the
        // shape that produced the silent self-exclusion bug in AuthRoleExclusionDao, and the row
        // counts here are tiny.
        return (search(started) + search(ended)).distinctBy { it.id }
    }

    /**
     * A revoked membership is gone as far as permission resolution is concerned; the row survives
     * only so the chain stays walkable and the audit trail stays complete.
     */
    private fun AuthGroupUser.isLive(): Boolean = revoked != true

    /** A membership is in force at [now] when now is within [start, end] (NULL bounds are open). */
    private fun isActiveAt(membership: AuthGroupUser, now: LocalDateTime): Boolean {
        val start = membership.startTime
        val end = membership.endTime
        return (start == null || !start.isAfter(now)) && (end == null || !end.isBefore(now))
    }

    /**
     * Deletes the relation matching the given group and user IDs.
     *
     * @param groupId group ID
     * @param userId user ID
     * @return number of rows deleted
     */
    fun deleteByGroupIdAndUserId(groupId: String, userId: String): Int {
        val criteria = Criteria.and(
            AuthGroupUser::groupId eq groupId,
            AuthGroupUser::userId eq userId
        )
        return batchDeleteCriteria(criteria)
    }


}
