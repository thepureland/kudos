package io.kudos.ms.user.core.account.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.core.account.model.po.UserOrgUser


/**
 * Organization-user association service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserOrgUserService : IBaseCrudService<String, UserOrgUser> {


    /**
     * Get the set of user ids for the given organization id.
     *
     * @param orgId organization id
     * @return user id set
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserIdsByOrgId(orgId: String): Set<String>

    /**
     * Get the set of organization ids for the given user id.
     *
     * @param userId user id
     * @return organization id set
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgIdsByUserId(userId: String): Set<String>

    /**
     * Batch-bind users to an organization.
     *
     * @param orgId organization id
     * @param userIds user id collection
     * @param orgAdmin whether the users are organization admins; defaults to false
     * @return number of bindings successfully created
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(orgId: String, userIds: Collection<String>, orgAdmin: Boolean = false): Int

    /**
     * Unbind a user from an organization.
     *
     * @param orgId organization id
     * @param userId user id
     * @return whether the unbind succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(orgId: String, userId: String): Boolean

    /**
     * Check whether the association exists.
     *
     * @param orgId organization id
     * @param userId user id
     * @return true if it exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(orgId: String, userId: String): Boolean

    /**
     * Set or clear the organization admin flag.
     *
     * @param orgId organization id
     * @param userId user id
     * @param isAdmin whether the user is an admin
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun setOrgAdmin(orgId: String, userId: String, isAdmin: Boolean): Boolean


}
