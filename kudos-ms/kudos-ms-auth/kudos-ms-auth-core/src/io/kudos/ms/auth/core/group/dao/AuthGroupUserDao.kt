package io.kudos.ms.auth.core.group.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.model.table.AuthGroupUsers
import org.springframework.stereotype.Repository


/**
 * Group-user relation DAO.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class AuthGroupUserDao : BaseCrudDao<String, AuthGroupUser, AuthGroupUsers>() {


    /**
     * Checks whether a group-user relation exists.
     *
     * @param groupId group ID
     * @param userId user ID
     * @return true if the relation exists
     * @author AI: Codex
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
     * Returns the user IDs bound to the given group.
     *
     * @param groupId group ID
     * @return set of user IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchUserIdsByGroupId(groupId: String): Set<String> {
        val criteria = Criteria(AuthGroupUser::groupId eq groupId)
        val userIds = searchProperty(criteria, AuthGroupUser::userId)
        return userIds.toSet()
    }

    /**
     * Returns the group IDs the given user belongs to.
     *
     * @param userId user ID
     * @return set of group IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchGroupIdsByUserId(userId: String): Set<String> {
        val criteria = Criteria(AuthGroupUser::userId eq userId)
        val groupIds = searchProperty(criteria, AuthGroupUser::groupId)
        return groupIds.toSet()
    }

    /**
     * Loads every group-user relation grouped by user ID into a "userId -> groupIds" map.
     *
     * @return map of user id to its list of group ids
     */
    fun searchAllUserIdToGroupIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.groupId } }
    }

    /**
     * Loads every group-user relation grouped by group ID into a "groupId -> userIds" map.
     *
     * @return map of group id to its list of user ids
     */
    fun searchAllGroupIdToUserIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.userId } }
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
