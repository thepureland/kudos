package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.po.AuthSecurityEventNotificationRoute
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object AuthSecurityEventNotificationRoutes :
    StringIdTable<AuthSecurityEventNotificationRoute>("auth_security_event_notification_route") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var notificationType = varchar("notification_type").bindTo { it.notificationType }
    var appliesTo = varchar("applies_to").bindTo { it.appliesTo }
    var routeCode = varchar("route_code").bindTo { it.routeCode }
    var destination = varchar("destination").bindTo { it.destination }
    var channels = varchar("channels").bindTo { it.channels }
    var responderUserIds = varchar("responder_user_ids").bindTo { it.responderUserIds }
    var responderRosterCode = varchar("responder_roster_code").bindTo { it.responderRosterCode }
    var includeAssignee = boolean("include_assignee").bindTo { it.includeAssignee }
    var enabled = boolean("enabled").bindTo { it.enabled }
    var fallbackBehavior = varchar("fallback_behavior").bindTo { it.fallbackBehavior }
    var configVersion = long("config_version").bindTo { it.configVersion }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
