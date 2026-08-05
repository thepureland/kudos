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
 * @author AI: Claude
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
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
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
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
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
        // Code-addressed bindings carry a null resource_id; they are consumed by the decision layer, not here.
        val list = searchProperty(criteria, AuthRoleResource::resourceId)
        return list.filterNotNull().map { it.trim() }.distinct().toSet()
    }

    /**
     * Returns the full binding rows for [roleIds] — not just the resource ids.
     *
     * The decision point needs the effect and the condition too, which the id-only projections
     * deliberately drop (they exist for menu rendering, where every binding is an unconditional
     * ALLOW by construction).
     *
     * @param roleIds the roles to read bindings for
     * @return every binding row belonging to those roles
     */
    open fun searchBindingsByRoleIds(roleIds: Collection<String>): List<AuthRoleResource> {
        if (roleIds.isEmpty()) return emptyList()
        val criteria = Criteria(AuthRoleResource::roleId inList roleIds.toList())
        return search(criteria)
    }

    /**
     * Returns all role-resource relations grouped by role id as "role id -> list of resource ids".
     *
     * @return Map<role id, List<resource id>>
     */
    fun searchAllRoleIdToResourceIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.mapNotNull { it.resourceId?.trim() } }
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


}
