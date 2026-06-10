package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.model.table.AuthRoleResources
import org.springframework.stereotype.Repository


/**
 * Role-resource relation DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class AuthRoleResourceDao : BaseCrudDao<String, AuthRoleResource, AuthRoleResources>() {


    /**
     * Checks whether the relation exists.
     *
     * @param roleId role id
     * @param resourceId resource id
     * @return true if it exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, resourceId: String): Boolean {
        val criteria = Criteria(AuthRoleResource::roleId eq roleId)
            .addAnd(AuthRoleResource::resourceId eq resourceId)
        return count(criteria) > 0
    }

    /**
     * Queries role IDs by resource id.
     *
     * @param resourceId resource id
     * @return set of role IDs
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun searchRoleIdsByResourceId(resourceId: String): Set<String> {
        val criteria = Criteria(AuthRoleResource::resourceId eq resourceId)
        val roleIds = searchProperty(criteria, AuthRoleResource::roleId)
        return roleIds.toSet()
    }

    /**
     * Queries deduplicated resource IDs for a list of role IDs.
     *
     * @param roleIds collection of role ids
     * @return Set<resource id>
     */
    fun searchResourceIdsByRoleIds(roleIds: Collection<String>): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val criteria = Criteria(AuthRoleResource::roleId inList roleIds.toList())
        val list = searchProperty(criteria, AuthRoleResource::resourceId)
        return list.map { it.trim() }.distinct().toSet()
    }

    /**
     * Returns all role-resource relations grouped by role id as "role id -> list of resource ids".
     *
     * @return Map<role id, List<resource id>>
     */
    fun searchAllRoleIdToResourceIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.map { it.resourceId.trim() } }
    }

    /**
     * Deletes a relation by role id and resource id.
     *
     * @param roleId role id
     * @param resourceId resource id
     * @return number of rows deleted
     */
    fun deleteByRoleIdAndResourceId(roleId: String, resourceId: String): Int {
        val criteria = Criteria(AuthRoleResource::roleId eq roleId)
            .addAnd(AuthRoleResource::resourceId eq resourceId)
        return batchDeleteCriteria(criteria)
    }

    /**
     * Deletes every resource grant of a role. Used by the role-delete cascade so a removed role
     * leaves no orphan resource grants behind.
     *
     * @param roleId role id
     * @return number of rows deleted
     */
    open fun deleteByRoleId(roleId: String): Int {
        val criteria = Criteria(AuthRoleResource::roleId eq roleId)
        return batchDeleteCriteria(criteria)
    }


}
