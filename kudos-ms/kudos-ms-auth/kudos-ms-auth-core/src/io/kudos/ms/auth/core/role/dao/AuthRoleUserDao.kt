package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.model.table.AuthRoleUsers
import org.springframework.stereotype.Repository


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
     * Queries role IDs by user id.
     *
     * @param userId user id
     * @return List<role id>
     */
    fun searchRoleIdsByUserId(userId: String): List<String> {
        val criteria = Criteria(AuthRoleUser::userId eq userId)
        return searchProperty(criteria, AuthRoleUser::roleId).filterNotNull()
    }

    /**
     * Returns all role-user relations grouped by user id as "user id -> list of role ids".
     *
     * @return Map<user id, List<role id>>
     */
    fun searchAllUserIdToRoleIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.roleId } }
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
