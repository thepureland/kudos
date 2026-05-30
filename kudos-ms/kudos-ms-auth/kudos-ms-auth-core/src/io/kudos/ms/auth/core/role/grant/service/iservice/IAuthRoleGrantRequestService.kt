package io.kudos.ms.auth.core.role.grant.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.core.role.grant.model.po.AuthRoleGrantRequest

/**
 * Service interface for the role-grant approval workflow.
 *
 * Lifecycle: PENDING → APPROVED | REJECTED | CANCELLED.
 *  - [submit]  creates a PENDING request (one open request per role+user).
 *  - [approve] flips PENDING → APPROVED and performs the actual bind via the role-user service
 *              (so SoD checks still apply — an approval can fail if it would create a conflict).
 *  - [reject]  flips PENDING → REJECTED (no bind).
 *  - [cancel]  flips a requester's own PENDING → CANCELLED.
 *
 * Approver authorisation is NOT modelled in data — it's enforced at the API layer (whoever can
 * call the admin endpoint may approve). A richer "approver role" model is a follow-up.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleGrantRequestService : IBaseCrudService<String, AuthRoleGrantRequest> {

    /**
     * Submit a new grant request. Validates the role and user exist, the user doesn't already
     * hold the role, and there's no open PENDING request for the same pair. The tenant is taken
     * from the role.
     *
     * @return the new request id.
     * @throws IllegalArgumentException on validation failure (missing role, duplicate pending,
     *   user already holds the role).
     */
    fun submit(roleId: String, userId: String, reason: String?): String

    /**
     * Approve a PENDING request: flip status to APPROVED and bind the role to the user.
     * The bind reuses the role-user service, so SoD constraints are enforced — if the grant
     * would violate one, the approval fails and the request stays PENDING.
     *
     * @return true on success.
     * @throws IllegalArgumentException if the request is missing or not PENDING.
     */
    fun approve(id: String, comment: String?): Boolean

    /**
     * Reject a PENDING request: flip status to REJECTED, no bind.
     *
     * @return true on success.
     * @throws IllegalArgumentException if the request is missing or not PENDING.
     */
    fun reject(id: String, comment: String?): Boolean

    /**
     * Cancel a PENDING request (intended for the original requester). Flips status to CANCELLED.
     *
     * @return true on success.
     * @throws IllegalArgumentException if the request is missing or not PENDING.
     */
    fun cancel(id: String): Boolean
}
