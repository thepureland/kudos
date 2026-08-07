package io.kudos.ms.auth.core.group.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.table.AuthGroupRoles
import org.springframework.stereotype.Repository


/**
 * Group-role relation DAO.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class AuthGroupRoleDao : BaseCrudDao<String, AuthGroupRole, AuthGroupRoles>() {


    /**
     * Checks whether a group-role relation exists.
     *
     * @param groupId group ID
     * @param roleId role ID
     * @return true if the relation exists
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, roleId: String): Boolean {
        val criteria = Criteria.and(
            AuthGroupRole::groupId eq groupId,
            AuthGroupRole::roleId eq roleId
        )
        return count(criteria) > 0
    }

    /**
     * Returns the role IDs bound to the given group.
     *
     * @param groupId group ID
     * @return set of role IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchRoleIdsByGroupId(groupId: String): Set<String> {
        val criteria = Criteria(AuthGroupRole::groupId eq groupId)
        return search(criteria).filter { it.revoked != true }.map { it.roleId }.toSet()
    }

    /**
     * Returns the group IDs the given role belongs to.
     *
     * @param roleId role ID
     * @return set of group IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchGroupIdsByRoleId(roleId: String): Set<String> {
        val criteria = Criteria(AuthGroupRole::roleId eq roleId)
        return search(criteria).filter { it.revoked != true }.map { it.groupId }.toSet()
    }

    /**
     * Loads every group-role relation grouped by group ID into a "groupId -> roleIds" map.
     *
     * @return map of group id to its list of role ids
     */
    fun searchAllGroupIdToRoleIdsForCache(): Map<String, List<String>> {
        val all = allSearch().filter { it.revoked != true }
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.roleId } }
    }

    /** Every binding row of a group, including revoked rows used for audit and reinstatement. */
    open fun searchBindingsByGroupId(groupId: String): List<AuthGroupRole> =
        search(Criteria(AuthGroupRole::groupId eq groupId))

    /**
     * Deletes the relation matching the given group and role IDs.
     *
     * @param groupId group ID
     * @param roleId role ID
     * @return number of rows deleted
     */
    fun deleteByGroupIdAndRoleId(groupId: String, roleId: String): Int {
        val criteria = Criteria.and(
            AuthGroupRole::groupId eq groupId,
            AuthGroupRole::roleId eq roleId
        )
        return batchDeleteCriteria(criteria)
    }

    /**
     * Deletes every group binding of a role. Used by the role-delete cascade so a removed role
     * stops being inherited through any group.
     *
     * @param roleId role ID
     * @return number of rows deleted
     */
    open fun deleteByRoleId(roleId: String): Int {
        val criteria = Criteria(AuthGroupRole::roleId eq roleId)
        return batchDeleteCriteria(criteria)
    }

    /** Deletes every role binding of a group during group deletion. */
    open fun deleteByGroupId(groupId: String): Int =
        batchDeleteCriteria(Criteria(AuthGroupRole::groupId eq groupId))


}
