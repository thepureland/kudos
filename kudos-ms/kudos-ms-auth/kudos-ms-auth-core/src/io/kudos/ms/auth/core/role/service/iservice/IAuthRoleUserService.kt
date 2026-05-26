package io.kudos.ms.auth.core.role.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser


/**
 * Role-User relation business interface
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IAuthRoleUserService : IBaseCrudService<String, AuthRoleUser> {


    /**
     * Get the set of user IDs by role ID
     *
     * @param roleId Role ID
     * @return Set of user IDs
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserIdsByRoleId(roleId: String): Set<String>

    /**
     * Get the set of role IDs by user ID
     *
     * @param userId User ID
     * @return Set of role IDs
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleIdsByUserId(userId: String): Set<String>

    /**
     * Batch bind role-user relations
     *
     * @param roleId Role ID
     * @param userIds Set of user IDs
     * @return Number of successfully bound entries
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(roleId: String, userIds: Collection<String>): Int

    /**
     * Unbind role-user relation
     *
     * @param roleId Role ID
     * @param userId User ID
     * @return Whether unbinding succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(roleId: String, userId: String): Boolean

    /**
     * Check whether the relation exists
     *
     * @param roleId Role ID
     * @param userId User ID
     * @return Whether it exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean


}
