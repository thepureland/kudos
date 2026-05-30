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
}
