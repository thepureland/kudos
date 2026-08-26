package io.kudos.ms.auth.core.authentication.securityevent.notification.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotificationChannel
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object AuthSecurityEventNotificationChannels :
    StringIdTable<AuthSecurityEventNotificationChannel>("auth_security_event_notification_channel") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var notificationId = varchar("notification_id").bindTo { it.notificationId }
    var channel = varchar("channel").bindTo { it.channel }
    var status = varchar("status").bindTo { it.status }
    var attemptCount = int("attempt_count").bindTo { it.attemptCount }
    var lastErrorCode = varchar("last_error_code").bindTo { it.lastErrorCode }
    var settledAt = datetime("settled_at").bindTo { it.settledAt }
}
