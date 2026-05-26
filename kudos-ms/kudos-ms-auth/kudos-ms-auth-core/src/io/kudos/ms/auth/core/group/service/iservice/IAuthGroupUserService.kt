package io.kudos.ms.auth.core.group.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser


/**
 * Group-user relation service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IAuthGroupUserService : IBaseCrudService<String, AuthGroupUser> {


    /**
     * Returns the user IDs bound to the given group.
     *
     * @param groupId group ID
     * @return set of user IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getUserIdsByGroupId(groupId: String): Set<String>

    /**
     * Returns the group IDs the given user belongs to.
     *
     * @param userId user ID
     * @return set of group IDs
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getGroupIdsByUserId(userId: String): Set<String>

    /**
     * Batch-binds a group to multiple users.
     *
     * @param groupId group ID
     * @param userIds user IDs to bind
     * @return number of bindings created
     * @author AI: Codex
     * @since 1.0.0
     */
    fun batchBind(groupId: String, userIds: Collection<String>): Int

    /**
     * Unbinds a group-user relation.
     *
     * @param groupId group ID
     * @param userId user ID
     * @return true if the relation was removed
     * @author AI: Codex
     * @since 1.0.0
     */
    fun unbind(groupId: String, userId: String): Boolean

    /**
     * Checks whether a group-user relation exists.
     *
     * @param groupId group ID
     * @param userId user ID
     * @return true if the relation exists
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, userId: String): Boolean


}
