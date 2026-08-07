package io.kudos.ms.auth.core.role.grant.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.grant.enums.GrantRequestStatus
import io.kudos.ms.auth.core.role.grant.model.po.AuthRoleGrantRequest
import io.kudos.ms.auth.core.role.grant.model.table.AuthRoleGrantRequests
import org.springframework.stereotype.Repository

/**
 * DAO for role-grant approval requests.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthRoleGrantRequestDao : BaseCrudDao<String, AuthRoleGrantRequest, AuthRoleGrantRequests>() {

    /**
     * Returns the existing PENDING request for the given (role, user), if any.
     * Used by the submit path to enforce "one open request per role+user".
     */
    open fun findPending(roleId: String, userId: String): AuthRoleGrantRequest? {
        val criteria = Criteria.and(
            AuthRoleGrantRequest::roleId eq roleId,
            AuthRoleGrantRequest::userId eq userId,
            AuthRoleGrantRequest::status eq GrantRequestStatus.PENDING.name,
        )
        return search(criteria).firstOrNull()
    }

    /**
     * Atomically moves a request out of PENDING. The status predicate is the concurrency guard:
     * exactly one racing decision can affect the row.
     */
    open fun transitionIfPending(request: AuthRoleGrantRequest): Boolean {
        val criteria = Criteria.and(
            AuthRoleGrantRequest::id eq request.id,
            AuthRoleGrantRequest::status eq GrantRequestStatus.PENDING.name,
        )
        return batchUpdateProperties(
            criteria,
            mapOf(
                AuthRoleGrantRequest::status.name to request.status,
                AuthRoleGrantRequest::approverId.name to request.approverId,
                AuthRoleGrantRequest::decisionComment.name to request.decisionComment,
                AuthRoleGrantRequest::decisionTime.name to request.decisionTime,
            ),
        ) == 1
    }
}
