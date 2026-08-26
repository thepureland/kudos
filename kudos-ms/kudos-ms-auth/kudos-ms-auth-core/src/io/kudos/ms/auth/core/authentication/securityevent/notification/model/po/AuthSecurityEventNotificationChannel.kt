package io.kudos.ms.auth.core.authentication.securityevent.notification.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * One settled channel outcome of a durable notification.
 *
 * Only terminal outcomes exist here. A channel that failed in a way worth retrying leaves no row, so the
 * ledger reads as exactly one thing: the channels this notification must never be sent on again.
 */
interface AuthSecurityEventNotificationChannel : IDbEntity<String, AuthSecurityEventNotificationChannel> {
    companion object : DbEntityFactory<AuthSecurityEventNotificationChannel>()

    var tenantId: String
    var notificationId: String
    var channel: String
    var status: String
    var attemptCount: Int
    var lastErrorCode: String?
    var settledAt: LocalDateTime
}
