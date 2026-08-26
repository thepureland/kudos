package io.kudos.ms.auth.core.authentication.securityevent.notification.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object AuthSecurityEventNotifications :
    StringIdTable<AuthSecurityEventNotification>("auth_security_event_notification") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var eventId = varchar("event_id").bindTo { it.eventId }
    var notificationType = varchar("notification_type").bindTo { it.notificationType }
    var escalationLevel = int("escalation_level").bindTo { it.escalationLevel }
    var recipientUserId = varchar("recipient_user_id").bindTo { it.recipientUserId }
    var dueAt = datetime("due_at").bindTo { it.dueAt }
    var escalatedAt = datetime("escalated_at").bindTo { it.escalatedAt }
    var status = varchar("status").bindTo { it.status }
    var attemptCount = int("attempt_count").bindTo { it.attemptCount }
    var nextAttemptAt = datetime("next_attempt_at").bindTo { it.nextAttemptAt }
    var leaseOwner = varchar("lease_owner").bindTo { it.leaseOwner }
    var leaseUntil = datetime("lease_until").bindTo { it.leaseUntil }
    var deliveredAt = datetime("delivered_at").bindTo { it.deliveredAt }
    var lastErrorCode = varchar("last_error_code").bindTo { it.lastErrorCode }
    var replayCount = int("replay_count").bindTo { it.replayCount }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
