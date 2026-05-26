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
        val roleIds = searchProperty(criteria, AuthGroupRole::roleId)
        return roleIds.toSet()
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
        val groupIds = searchProperty(criteria, AuthGroupRole::groupId)
        return groupIds.toSet()
    }

    /**
     * Loads every group-role relation grouped by group ID into a "groupId -> roleIds" map.
     *
     * @return map of group id to its list of role ids
     */
    fun searchAllGroupIdToRoleIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.roleId } }
    }

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


}
