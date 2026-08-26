package io.kudos.ms.auth.core.authentication.securityevent.notification.service.impl

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationSummary
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationChannelDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationAdminService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
open class AuthSecurityEventNotificationAdminService(
    private val dao: AuthSecurityEventNotificationDao,
    private val channelDao: AuthSecurityEventNotificationChannelDao,
    private val clock: Clock = Clock.systemUTC(),
) : IAuthSecurityEventNotificationAdminService {

    @Transactional(readOnly = true)
    override fun listDead(
        tenantId: String,
        eventId: String?,
        limit: Int,
    ): List<AuthSecurityEventNotificationSummary> {
        requireIdentifier(tenantId, "AUTH_SECURITY_EVENT_NOTIFICATION_TENANT_INVALID")
        eventId?.let { requireIdentifier(it, "AUTH_SECURITY_EVENT_NOTIFICATION_EVENT_ID_INVALID") }
        if (limit !in 1..MAX_QUERY_LIMIT) fail("AUTH_SECURITY_EVENT_NOTIFICATION_LIMIT_INVALID")
        return dao.findDead(tenantId, eventId, limit).map { it.toSummary() }
    }

    @Transactional
    override fun replay(command: AuthSecurityEventNotificationReplayCommand): AuthSecurityEventNotificationSummary {
        requireIdentifier(command.tenantId, "AUTH_SECURITY_EVENT_NOTIFICATION_TENANT_INVALID")
        requireIdentifier(command.notificationId, "AUTH_SECURITY_EVENT_NOTIFICATION_ID_INVALID")
        requireIdentifier(command.actorUserId, "AUTH_SECURITY_EVENT_NOTIFICATION_ACTOR_INVALID")
        val reason = command.reason.trim().also {
            if (it.isBlank() || it.length > MAX_REASON_LENGTH || it.any(Char::isISOControl)) {
                fail("AUTH_SECURITY_EVENT_NOTIFICATION_REPLAY_REASON_INVALID")
            }
        }
        val current = dao.findByTenantAndId(command.tenantId, command.notificationId)
            ?: fail("AUTH_SECURITY_EVENT_NOTIFICATION_NOT_FOUND")
        if (parseStatus(current.status) != AuthSecurityEventNotificationStatusEnum.DEAD) {
            fail("AUTH_SECURITY_EVENT_NOTIFICATION_STATE_CONFLICT")
        }
        val now = LocalDateTime.now(clock)
        if (!dao.replay(command.tenantId, command.notificationId, now)) {
            fail("AUTH_SECURITY_EVENT_NOTIFICATION_STATE_CONFLICT")
        }
        // An operator replays because they decided the previous outcome was wrong — including channels that
        // were permanently refused. Carrying those settlements forward would make the replay deliver nothing.
        channelDao.deleteByNotification(command.tenantId, command.notificationId)
        dao.insertReplayAudit(
            id = UUID.randomUUID().toString(),
            tenantId = command.tenantId,
            notificationId = command.notificationId,
            actorUserId = command.actorUserId,
            reason = reason,
            replayedAt = now,
        )
        return dao.findByTenantAndId(command.tenantId, command.notificationId)?.toSummary()
            ?: fail("AUTH_SECURITY_EVENT_NOTIFICATION_NOT_FOUND")
    }

    private fun AuthSecurityEventNotification.toSummary() = AuthSecurityEventNotificationSummary(
        id = id,
        eventId = eventId,
        notificationType = runCatching { AuthSecurityEventNotificationTypeEnum.valueOf(notificationType) }
            .getOrElse { fail("AUTH_SECURITY_EVENT_NOTIFICATION_TYPE_INVALID") },
        escalationLevel = escalationLevel,
        recipientUserId = recipientUserId,
        dueAt = dueAt,
        escalatedAt = escalatedAt,
        status = parseStatus(status),
        attemptCount = attemptCount,
        lastErrorCode = lastErrorCode,
        replayCount = replayCount,
        deliveredAt = deliveredAt,
        createTime = createTime,
        updateTime = updateTime,
    )

    private fun parseStatus(value: String): AuthSecurityEventNotificationStatusEnum = runCatching {
        AuthSecurityEventNotificationStatusEnum.valueOf(value)
    }.getOrElse { fail("AUTH_SECURITY_EVENT_NOTIFICATION_STATUS_INVALID") }

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > 36 || value.any(Char::isISOControl)) fail(errorCode)
    }

    private fun fail(errorCode: String): Nothing = throw AuthSecurityEventException(errorCode)

    private companion object {
        const val MAX_QUERY_LIMIT = 500
        const val MAX_REASON_LENGTH = 512
    }
}
