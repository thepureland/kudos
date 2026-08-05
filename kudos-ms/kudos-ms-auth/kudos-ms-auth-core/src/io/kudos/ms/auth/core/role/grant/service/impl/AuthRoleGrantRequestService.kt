package io.kudos.ms.auth.core.role.grant.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.grant.enums.GrantRequestStatus
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.grant.dao.AuthRoleGrantRequestDao
import io.kudos.ms.auth.core.role.grant.model.po.AuthRoleGrantRequest
import io.kudos.ms.auth.core.role.grant.service.iservice.IAuthRoleGrantRequestService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Role-grant approval workflow implementation.
 *
 * State machine guard: every action ([approve]/[reject]/[cancel]) first re-reads the row and
 * rejects if it's no longer PENDING, so two approvers racing on the same request can't both
 * succeed (the second sees a terminal state).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleGrantRequestService(
    dao: AuthRoleGrantRequestDao,
) : BaseCrudService<String, AuthRoleGrantRequest, AuthRoleGrantRequestDao>(dao),
    IAuthRoleGrantRequestService {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authzProperties: AuthzProperties

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun submit(roleId: String, userId: String, reason: String?): String {
        require(roleId.isNotBlank()) { "roleId must not be blank." }
        require(userId.isNotBlank()) { "userId must not be blank." }

        val role = authRoleDao.get(roleId)
            ?: throw IllegalArgumentException("Role not found: $roleId")

        // No point requesting a role the user already has.
        require(!authRoleUserService.exists(roleId, userId)) {
            "User $userId already holds role $roleId."
        }
        // One open request per (role, user).
        require(dao.findPending(roleId, userId) == null) {
            "A pending grant request for role $roleId and user $userId already exists."
        }

        val now = LocalDateTime.now()
        val requesterId = CurrentUserKit.currentUserIdOrNull()
        val request = AuthRoleGrantRequest {
            this.roleId = roleId
            this.userId = userId
            this.tenantId = role.tenantId
            this.status = GrantRequestStatus.PENDING.name
            this.reason = reason
            this.requesterId = requesterId
            this.requestTime = now
        }
        val id = dao.insert(request)
        log.debug("Submitted grant request $id: role=$roleId user=$userId requester=$requesterId")
        return id
    }

    @Transactional
    override fun approve(id: String, comment: String?): Boolean {
        val request = loadPendingOrThrow(id)
        val approverId = CurrentUserKit.currentUserIdOrNull()
        assertMayApprove(approverId, request.roleId)

        // Perform the actual bind first; if it throws (e.g. SoD violation) the whole transaction
        // rolls back and the request stays PENDING — the approver sees the error. This is the one
        // path that may bind a role marked `approval_required`, because it is the one path on which
        // a second person has signed off.
        authRoleUserService.bindApproved(request.roleId, request.userId)

        request.status = GrantRequestStatus.APPROVED.name
        request.approverId = approverId
        request.decisionComment = comment
        request.decisionTime = LocalDateTime.now()
        val success = dao.update(request)
        log.debug("Approved grant request $id (role=${request.roleId} user=${request.userId}); bind performed.")
        return success
    }

    @Transactional
    override fun reject(id: String, comment: String?): Boolean {
        val request = loadPendingOrThrow(id)
        request.status = GrantRequestStatus.REJECTED.name
        request.approverId = CurrentUserKit.currentUserIdOrNull()
        request.decisionComment = comment
        request.decisionTime = LocalDateTime.now()
        val success = dao.update(request)
        log.debug("Rejected grant request $id.")
        return success
    }

    @Transactional
    override fun cancel(id: String): Boolean {
        val request = loadPendingOrThrow(id)
        request.status = GrantRequestStatus.CANCELLED.name
        request.decisionTime = LocalDateTime.now()
        val success = dao.update(request)
        log.debug("Cancelled grant request $id.")
        return success
    }

    /**
     * Asserts that [approverId] is entitled to decide a request for [roleId].
     *
     * The rule: an approver must be somebody who **could have granted the role themselves** — they
     * hold it through a live direct grant that still carries delegation allowance, or they are a
     * platform administrator. Anything looser makes the approval step theatre: a workflow that any
     * authenticated user can sign off adds a row to the table and nothing to the security of the
     * system.
     *
     * The delegation chain is what supplies the answer, which is why no separate approver-registry
     * table is needed: the people upstream of a role are already recorded.
     */
    private fun assertMayApprove(approverId: String?, roleId: String) {
        requireNotNull(approverId) { "an approval must be attributable to a logged-in approver." }
        if (isPlatformAdmin(approverId)) return
        val ownGrant = authRoleUserDao.findLiveDirectGrant(roleId, approverId)
        require(ownGrant != null && (ownGrant.delegableDepth ?: 0) >= 1) {
            "approver ${approverId} may not decide requests for role ${roleId}: approving requires " +
                "being able to grant it, i.e. holding it through a delegable direct grant."
        }
    }

    private fun isPlatformAdmin(userId: String): Boolean {
        val adminCodes = authzProperties.platformAdminRoleCodes
        if (adminCodes.isEmpty()) return false
        return authRoleUserDao.searchRoleIdsByUserId(userId)
            .any { roleId -> authRoleDao.get(roleId)?.code in adminCodes }
    }

    /** Re-read the row and assert it's still PENDING; the guard against double-decision races. */
    private fun loadPendingOrThrow(id: String): AuthRoleGrantRequest {
        val request = dao.get(id)
            ?: throw IllegalArgumentException("Grant request not found: $id")
        val status = GrantRequestStatus.fromString(request.status)
        require(status == GrantRequestStatus.PENDING) {
            "Grant request $id is already ${request.status}; only PENDING requests can be acted on."
        }
        return request
    }
}
