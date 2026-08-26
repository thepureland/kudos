package io.kudos.ms.auth.core.authentication.securityevent.notification.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Durable, channel-neutral notification produced by a security-event workflow transaction. */
interface AuthSecurityEventNotification : IDbEntity<String, AuthSecurityEventNotification> {
    companion object : DbEntityFactory<AuthSecurityEventNotification>()

    var tenantId: String
    var eventId: String
    var notificationType: String
    var escalationLevel: Int
    var recipientUserId: String?
    var dueAt: LocalDateTime?
    var escalatedAt: LocalDateTime
    var status: String
    var attemptCount: Int
    var nextAttemptAt: LocalDateTime?
    var leaseOwner: String?
    var leaseUntil: LocalDateTime?
    var deliveredAt: LocalDateTime?
    var lastErrorCode: String?
    var replayCount: Int
    var createTime: LocalDateTime
    var updateTime: LocalDateTime
}
