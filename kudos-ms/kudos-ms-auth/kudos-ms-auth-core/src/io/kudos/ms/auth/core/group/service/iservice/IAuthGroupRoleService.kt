package io.kudos.ms.auth.core.group.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole


/**
 * Group-role relation service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IAuthGroupRoleService : IBaseCrudService<String, AuthGroupRole> {


    /**
     * Returns the role IDs bound to the given group.
     *
     * @param groupId group ID
     * @return set of role IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getRoleIdsByGroupId(groupId: String): Set<String>

    /**
     * Returns the group IDs the given role belongs to.
     *
     * @param roleId role ID
     * @return set of group IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getGroupIdsByRoleId(roleId: String): Set<String>

    /**
     * Batch-binds a group to multiple roles.
     *
     * @param groupId group ID
     * @param roleIds role IDs to bind
     * @return number of bindings created
     * @author AI: Codex
     * @since 1.0.0
     */
    fun batchBind(groupId: String, roleIds: Collection<String>): Int

    /**
     * Unbinds a group-role relation.
     *
     * @param groupId group ID
     * @param roleId role ID
     * @return true if the relation was removed
     * @author AI: Codex
     * @since 1.0.0
     */
    fun unbind(groupId: String, roleId: String): Boolean

    /**
     * Checks whether a group-role relation exists.
     *
     * @param groupId group ID
     * @param roleId role ID
     * @return true if the relation exists
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, roleId: String): Boolean


}
