package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One tenant notification route rule, unique per tenant, notification type and delivery shape. */
interface AuthSecurityEventNotificationRoute : IDbEntity<String, AuthSecurityEventNotificationRoute> {
    companion object : DbEntityFactory<AuthSecurityEventNotificationRoute>()

    var tenantId: String
    var notificationType: String
    var appliesTo: String
    var routeCode: String
    var destination: String
    var channels: String
    var responderUserIds: String?
    var responderRosterCode: String?
    var includeAssignee: Boolean
    var enabled: Boolean
    var fallbackBehavior: String
    var configVersion: Long
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}
