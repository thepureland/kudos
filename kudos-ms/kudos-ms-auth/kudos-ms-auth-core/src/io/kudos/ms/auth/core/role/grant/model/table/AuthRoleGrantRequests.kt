package io.kudos.ms.auth.core.role.grant.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.grant.model.po.AuthRoleGrantRequest
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Ktorm table mapping for [AuthRoleGrantRequest].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthRoleGrantRequests : StringIdTable<AuthRoleGrantRequest>("auth_role_grant_request") {

    var roleId = varchar("role_id").bindTo { it.roleId }
    var userId = varchar("user_id").bindTo { it.userId }
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var status = varchar("status").bindTo { it.status }
    var reason = varchar("reason").bindTo { it.reason }
    var requesterId = varchar("requester_id").bindTo { it.requesterId }
    var requestTime = datetime("request_time").bindTo { it.requestTime }
    var approverId = varchar("approver_id").bindTo { it.approverId }
    var decisionComment = varchar("decision_comment").bindTo { it.decisionComment }
    var decisionTime = datetime("decision_time").bindTo { it.decisionTime }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
