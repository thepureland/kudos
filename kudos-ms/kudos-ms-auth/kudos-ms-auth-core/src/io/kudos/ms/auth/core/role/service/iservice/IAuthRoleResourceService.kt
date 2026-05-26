package io.kudos.ms.auth.core.role.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource


/**
 * Role-Resource relation business interface
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IAuthRoleResourceService : IBaseCrudService<String, AuthRoleResource> {


    /**
     * Get the set of resource IDs by role ID
     *
     * @param roleId Role ID
     * @return Set of resource IDs
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getResourceIdsByRoleId(roleId: String): Set<String>

    /**
     * Get the set of role IDs by resource ID
     *
     * @param resourceId Resource ID
     * @return Set of role IDs
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleIdsByResourceId(resourceId: String): Set<String>

    /**
     * Batch bind role-resource relations
     *
     * @param roleId Role ID
     * @param resourceIds Set of resource IDs
     * @return Number of successfully bound entries
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(roleId: String, resourceIds: Collection<String>): Int

    /**
     * Unbind role-resource relation
     *
     * @param roleId Role ID
     * @param resourceId Resource ID
     * @return Whether unbinding succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(roleId: String, resourceId: String): Boolean

    /**
     * Check whether the relation exists
     *
     * @param roleId Role ID
     * @param resourceId Resource ID
     * @return Whether it exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, resourceId: String): Boolean


}
