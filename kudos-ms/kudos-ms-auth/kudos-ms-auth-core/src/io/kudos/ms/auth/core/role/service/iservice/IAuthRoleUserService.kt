package io.kudos.ms.auth.core.role.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.delegation.vo.RevokeImpactVo
import io.kudos.ms.auth.common.delegation.vo.RoleGrantRequest
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser


/**
 * Role-User relation business interface
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleUserService : IBaseCrudService<String, AuthRoleUser> {


    /**
     * Get the set of user IDs by role ID
     *
     * @param roleId Role ID
     * @return Set of user IDs
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun getUserIdsByRoleId(roleId: String): Set<String>

    /**
     * Get the set of role IDs by user ID
     *
     * @param userId User ID
     * @return Set of role IDs
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun getRoleIdsByUserId(userId: String): Set<String>

    /**
     * Batch bind role-user relations
     *
     * @param roleId Role ID
     * @param userIds Set of user IDs
     * @return Number of successfully bound entries
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun batchBind(roleId: String, userIds: Collection<String>): Int

    /**
     * Unbind role-user relation
     *
     * @param roleId Role ID
     * @param userId User ID
     * @return Whether unbinding succeeded
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun unbind(roleId: String, userId: String): Boolean

    /**
     * Check whether the relation exists
     *
     * @param roleId Role ID
     * @param userId User ID
     * @return Whether it exists
     * @author K
     * @author AI: Cursor
     * @author AI: Claude
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean

    /**
     * Binds a role whose `approval_required` demand has already been satisfied by the grant-request
     * workflow. The only caller is that workflow; everything else goes through [batchBind] and is
     * refused for such roles.
     *
     * @param roleId the approved role
     * @param userId the approved recipient
     * @return 1 when a binding was created, 0 when the principal already held the role
     */
    fun bindApproved(roleId: String, userId: String): Int

    /**
     * Hands a role to principals as a **delegated** grant, on behalf of [operatorId].
     *
     * The difference from [batchBind] is who is answerable for it. A bind is an administrative act
     * with no upstream; a delegation is somebody passing on authority they themselves were given,
     * which means it can be screened against what they hold ([io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService])
     * and, crucially, **withdrawn when they lose it**. The `parent_grant_id` written here is what
     * makes that possible later.
     *
     * @param request what to hand out, how far it may travel and how long it lasts
     * @param operatorId the principal exercising their delegation authority
     * @return the ids of the grants created; principals who already hold the role are skipped
     * @throws IllegalArgumentException if any recipient fails admission — a delegation names its
     *   recipients explicitly, so a rejected one aborts the call rather than being dropped silently
     */
    fun grant(request: RoleGrantRequest, operatorId: String): List<String>

    /**
     * Revokes a grant and every grant delegated from it, transitively.
     *
     * Soft: rows are marked, never deleted. A deleted chain cannot answer "who gave this away, and
     * what else did they give away" — the question that matters most at exactly the moment somebody
     * is being revoked.
     *
     * @param grantId the grant to withdraw
     * @param reason recorded on every row the cascade touches
     * @return the number of grants revoked, the root included
     */
    fun revoke(grantId: String, reason: String? = null): Int

    /**
     * What [revoke] would take away. Call it first and show it: the blast radius of a cascade is
     * invisible from the grant being revoked.
     *
     * @param grantId the grant that would be withdrawn
     * @return the impact summary; empty when the grant does not exist
     */
    fun getRevokeImpact(grantId: String): RevokeImpactVo

}
