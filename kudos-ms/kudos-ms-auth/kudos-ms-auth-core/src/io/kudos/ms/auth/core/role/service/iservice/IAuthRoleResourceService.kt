package io.kudos.ms.auth.core.role.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.role.vo.PermissionBindingRequest
import io.kudos.ms.auth.common.role.vo.PermissionBindingVo
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource


/**
 * Role-Resource relation business interface
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleResourceService : IBaseCrudService<String, AuthRoleResource> {


    /**
     * Get the set of resource IDs by role ID
     *
     * @param roleId Role ID
     * @return Set of resource IDs
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun getResourceIdsByRoleId(roleId: String): Set<String>

    /**
     * Get the set of role IDs by resource ID
     *
     * @param resourceId Resource ID
     * @return Set of role IDs
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun getRoleIdsByResourceId(resourceId: String): Set<String>

    /**
     * Batch bind role-resource relations
     *
     * @param roleId Role ID
     * @param resourceIds Set of resource IDs
     * @return Number of successfully bound entries
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun batchBind(roleId: String, resourceIds: Collection<String>): Int

    /**
     * Unbind role-resource relation
     *
     * @param roleId Role ID
     * @param resourceId Resource ID
     * @return Whether unbinding succeeded
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun unbind(roleId: String, resourceId: String): Boolean

    /**
     * Check whether the relation exists
     *
     * @param roleId Role ID
     * @param resourceId Resource ID
     * @return Whether it exists
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun exists(roleId: String, resourceId: String): Boolean

    /**
     * The role's **code-addressed** bindings — the ones that carry an effect and a condition.
     *
     * Kept separate from [getResourceIdsByRoleId] because the two are different shapes, not two
     * views of one thing: a resource binding is a node in a tree, this is a rule with its own
     * effect and gating.
     *
     * @param roleId role id
     * @return the bindings, code-addressed only
     */
    fun listPermissionBindings(roleId: String): List<PermissionBindingVo>

    /**
     * Creates or updates one code-addressed binding.
     *
     * Update-not-duplicate on (role, code): re-granting the same code is how an admin changes its
     * effect or condition, and leaving two rows whose combined meaning depends on evaluation order
     * would be a worse answer than either of them.
     *
     * @param request what to bind
     * @return the binding id
     */
    fun savePermissionBinding(request: PermissionBindingRequest): String

    /**
     * Removes one code-addressed binding.
     *
     * @param bindingId the binding to remove
     * @return whether a row was removed
     */
    fun removePermissionBinding(bindingId: String): Boolean

}
