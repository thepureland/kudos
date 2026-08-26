package io.kudos.ms.auth.core.authentication.securityevent.notification

import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.boot.context.properties.ConfigurationProperties

enum class AuthSecurityEventNotificationTypeEnum {
    SLA_ESCALATED,
}

enum class AuthSecurityEventNotificationStatusEnum {
    PENDING,
    PROCESSING,
    DELIVERED,
    DEAD,
}

data class AuthSecurityEventNotificationDelivery(
    val id: String,
    val tenantId: String,
    val eventId: String,
    val notificationType: AuthSecurityEventNotificationTypeEnum,
    val escalationLevel: Int,
    val recipientUserId: String?,
    val dueAt: LocalDateTime?,
    val escalatedAt: LocalDateTime,
    val attemptCount: Int,
    val leaseUntil: LocalDateTime,
)

data class AuthSecurityEventNotificationSummary(
    val id: String,
    val eventId: String,
    val notificationType: AuthSecurityEventNotificationTypeEnum,
    val escalationLevel: Int,
    val recipientUserId: String?,
    val dueAt: LocalDateTime?,
    val escalatedAt: LocalDateTime,
    val status: AuthSecurityEventNotificationStatusEnum,
    val attemptCount: Int,
    val lastErrorCode: String?,
    val replayCount: Int,
    val deliveredAt: LocalDateTime?,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
)

data class AuthSecurityEventNotificationReplayCommand(
    val tenantId: String,
    val notificationId: String,
    val actorUserId: String,
    val reason: String,
)

/** Terminal per-channel outcomes; a channel worth retrying stays out of the ledger entirely. */
enum class AuthSecurityEventNotificationChannelStatusEnum {
    DELIVERED,
    DEAD,
}

data class AuthSecurityEventNotificationChannelOutcome(
    val channel: AuthSecurityEventNotificationChannelEnum,
    val status: AuthSecurityEventNotificationChannelStatusEnum,
    val attemptCount: Int,
    val lastErrorCode: String?,
    val settledAt: LocalDateTime,
)

data class AuthSecurityEventNotificationDispatchResult(
    val claimedCount: Int,
    val deliveredCount: Int,
    val retryScheduledCount: Int,
    val deadCount: Int,
    val channelDeliveredCount: Int,
    val channelDeadCount: Int,
    val leaseLostCount: Int,
)

@ConfigurationProperties(prefix = "kudos.ms.auth.security-event.notification")
open class AuthSecurityEventNotificationProperties {
    var leaseDuration: Duration = Duration.ofMinutes(1)
    var maxAttempts: Int = 8
    var initialBackoff: Duration = Duration.ofSeconds(30)
    var maxBackoff: Duration = Duration.ofHours(1)
}
