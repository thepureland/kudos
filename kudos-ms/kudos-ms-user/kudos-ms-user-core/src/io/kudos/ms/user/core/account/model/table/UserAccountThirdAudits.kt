package io.kudos.ms.user.core.account.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.account.model.po.UserAccountThirdAudit
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/** Binding for the append-only external identity lifecycle audit table. */
object UserAccountThirdAudits : StringIdTable<UserAccountThirdAudit>("user_account_third_audit") {
    var bindingId = varchar("binding_id").bindTo { it.bindingId }
    var userId = varchar("user_id").bindTo { it.userId }
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var identityProviderId = varchar("identity_provider_id").bindTo { it.identityProviderId }
    var providerCode = varchar("provider_code").bindTo { it.providerCode }
    var subjectHash = varchar("subject_hash").bindTo { it.subjectHash }
    var action = varchar("action").bindTo { it.action }
    var success = boolean("success").bindTo { it.success }
    var reason = varchar("reason").bindTo { it.reason }
    var actorUserId = varchar("actor_user_id").bindTo { it.actorUserId }
    var operationReason = varchar("operation_reason").bindTo { it.operationReason }
    var beforeSnapshot = varchar("before_snapshot").bindTo { it.beforeSnapshot }
    var afterSnapshot = varchar("after_snapshot").bindTo { it.afterSnapshot }
    var eventTime = datetime("event_time").bindTo { it.eventTime }
}
