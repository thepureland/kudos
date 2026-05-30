package io.kudos.ms.auth.core.role.grant.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role-grant approval request database entity.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRoleGrantRequest : IDbEntity<String, AuthRoleGrantRequest> {

    companion object : DbEntityFactory<AuthRoleGrantRequest>()

    /** Role being requested. */
    var roleId: String

    /** User the role would be granted to. */
    var userId: String

    /** Tenant id (denormalised from the role for query scoping). */
    var tenantId: String

    /** PENDING / APPROVED / REJECTED / CANCELLED — stored as the enum name. */
    var status: String

    /** Requester's stated reason. */
    var reason: String?

    /** User id who submitted the request. */
    var requesterId: String?

    /** When the request was submitted. */
    var requestTime: LocalDateTime?

    /** User id who approved/rejected (null while pending). */
    var approverId: String?

    /** Approver's decision comment. */
    var decisionComment: String?

    /** When the decision was made (null while pending). */
    var decisionTime: LocalDateTime?

    var createUserId: String?
    var createUserName: String?
    var createTime: LocalDateTime?
    var updateUserId: String?
    var updateUserName: String?
    var updateTime: LocalDateTime?
}
