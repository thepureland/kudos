package io.kudos.ms.auth.core.instance.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.instance.model.po.AuthInstanceGrant
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Table mapping for [AuthInstanceGrant].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthInstanceGrants : StringIdTable<AuthInstanceGrant>("auth_instance_grant") {

    var principalId = varchar("principal_id").bindTo { it.principalId }
    var principalType = varchar("principal_type").bindTo { it.principalType }
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var resourceType = varchar("resource_type").bindTo { it.resourceType }
    var instanceId = varchar("instance_id").bindTo { it.instanceId }
    var action = varchar("action").bindTo { it.action }
    var effect = varchar("effect").bindTo { it.effect }
    var startTime = datetime("start_time").bindTo { it.startTime }
    var endTime = datetime("end_time").bindTo { it.endTime }
    var grantedBy = varchar("granted_by").bindTo { it.grantedBy }
    var revoked = boolean("revoked").bindTo { it.revoked }
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
