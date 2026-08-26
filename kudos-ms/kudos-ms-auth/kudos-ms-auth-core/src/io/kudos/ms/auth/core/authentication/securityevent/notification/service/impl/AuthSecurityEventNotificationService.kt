package io.kudos.ms.auth.core.authentication.securityevent.notification.service.impl

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationProperties
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelOutcome
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationChannelDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
open class AuthSecurityEventNotificationService(
    private val dao: AuthSecurityEventNotificationDao,
    private val channelDao: AuthSecurityEventNotificationChannelDao,
    private val properties: AuthSecurityEventNotificationProperties,
    private val clock: Clock = Clock.systemUTC(),
) : IAuthSecurityEventNotificationService {

    @Transactional
    override fun claimPending(workerId: String, limit: Int): List<AuthSecurityEventNotificationDelivery> {
        validateWorkerId(workerId)
        validateProperties()
        if (limit !in 1..MAX_CLAIM_LIMIT) fail("AUTH_SECURITY_EVENT_NOTIFICATION_LIMIT_INVALID")
        val now = LocalDateTime.now(clock)
        val leaseUntil = now.plus(properties.leaseDuration)
        return dao.findClaimableIds(now, limit).mapNotNull { id ->
            if (!dao.claim(id, workerId, now, leaseUntil)) return@mapNotNull null
            dao.get(id)?.toDelivery()
        }
    }

    @Transactional(readOnly = true)
    override fun settledChannels(notificationId: String): List<AuthSecurityEventNotificationChannelOutcome> {
        validateId(notificationId)
        return channelDao.findByNotification(notificationId).map { settled ->
            AuthSecurityEventNotificationChannelOutcome(
                channel = runCatching { AuthSecurityEventNotificationChannelEnum.valueOf(settled.channel) }
                    .getOrElse { fail("AUTH_SECURITY_EVENT_NOTIFICATION_CHANNEL_INVALID") },
                status = runCatching {
                    AuthSecurityEventNotificationChannelStatusEnum.valueOf(settled.status)
                }.getOrElse { fail("AUTH_SECURITY_EVENT_NOTIFICATION_CHANNEL_STATUS_INVALID") },
                attemptCount = settled.attemptCount,
                lastErrorCode = settled.lastErrorCode,
                settledAt = settled.settledAt,
            )
        }
    }

    /**
     * Written in its own transaction so a channel that did reach its recipient stays settled even when a later
     * channel of the same notification fails and the caller's transaction is rolled back. Losing that record
     * would mean re-sending a message somebody already received.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun settleChannel(
        notificationId: String,
        tenantId: String,
        channel: AuthSecurityEventNotificationChannelEnum,
        status: AuthSecurityEventNotificationChannelStatusEnum,
        attemptCount: Int,
        errorCode: String?,
    ): Boolean {
        validateId(notificationId)
        validateId(tenantId)
        if (attemptCount < 0) fail("AUTH_SECURITY_EVENT_NOTIFICATION_CHANNEL_ATTEMPT_INVALID")
        val normalizedError = when (status) {
            AuthSecurityEventNotificationChannelStatusEnum.DELIVERED -> null
            AuthSecurityEventNotificationChannelStatusEnum.DEAD ->
                normalizeErrorCode(errorCode ?: fail("AUTH_SECURITY_EVENT_NOTIFICATION_ERROR_CODE_INVALID"))
        }
        return channelDao.settleIfAbsent(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            notificationId = notificationId,
            channel = channel.name,
            status = status.name,
            attemptCount = attemptCount,
            errorCode = normalizedError,
            settledAt = LocalDateTime.now(clock),
        )
    }

    @Transactional
    override fun complete(notificationId: String, workerId: String): Boolean {
        validateId(notificationId)
        validateWorkerId(workerId)
        return dao.complete(notificationId, workerId, LocalDateTime.now(clock))
    }

    @Transactional
    override fun fail(notificationId: String, workerId: String, errorCode: String): Boolean {
        validateId(notificationId)
        validateWorkerId(workerId)
        validateProperties()
        val normalizedError = normalizeErrorCode(errorCode)
        val current = dao.get(notificationId) ?: return false
        val now = LocalDateTime.now(clock)
        val dead = current.attemptCount >= properties.maxAttempts
        val nextAttemptAt = if (dead) null else now.plus(backoff(current.attemptCount))
        val status = if (dead) {
            AuthSecurityEventNotificationStatusEnum.DEAD
        } else {
            AuthSecurityEventNotificationStatusEnum.PENDING
        }
        return dao.fail(notificationId, workerId, status.name, nextAttemptAt, normalizedError, now)
    }

    @Transactional
    override fun dead(notificationId: String, workerId: String, errorCode: String): Boolean {
        validateId(notificationId)
        validateWorkerId(workerId)
        val normalizedError = normalizeErrorCode(errorCode)
        return dao.fail(
            notificationId,
            workerId,
            AuthSecurityEventNotificationStatusEnum.DEAD.name,
            null,
            normalizedError,
            LocalDateTime.now(clock),
        )
    }

    private fun AuthSecurityEventNotification.toDelivery() = AuthSecurityEventNotificationDelivery(
        id = id,
        tenantId = tenantId,
        eventId = eventId,
        notificationType = runCatching { AuthSecurityEventNotificationTypeEnum.valueOf(notificationType) }
            .getOrElse { fail("AUTH_SECURITY_EVENT_NOTIFICATION_TYPE_INVALID") },
        escalationLevel = escalationLevel,
        recipientUserId = recipientUserId,
        dueAt = dueAt,
        escalatedAt = escalatedAt,
        attemptCount = attemptCount,
        leaseUntil = leaseUntil ?: fail("AUTH_SECURITY_EVENT_NOTIFICATION_LEASE_INVALID"),
    )

    private fun backoff(attemptCount: Int): Duration {
        val multiplier = 1L shl (attemptCount - 1).coerceIn(0, 30)
        val candidate = runCatching { properties.initialBackoff.multipliedBy(multiplier) }
            .getOrElse { properties.maxBackoff }
        return if (candidate > properties.maxBackoff) properties.maxBackoff else candidate
    }

    private fun validateProperties() {
        if (properties.maxAttempts !in 1..MAX_ATTEMPTS) fail("AUTH_SECURITY_EVENT_NOTIFICATION_CONFIG_INVALID")
        if (!properties.leaseDuration.isPositive || !properties.initialBackoff.isPositive ||
            !properties.maxBackoff.isPositive || properties.initialBackoff > properties.maxBackoff
        ) {
            fail("AUTH_SECURITY_EVENT_NOTIFICATION_CONFIG_INVALID")
        }
    }

    private fun validateWorkerId(value: String) {
        if (!WORKER_ID.matches(value)) fail("AUTH_SECURITY_EVENT_NOTIFICATION_WORKER_INVALID")
    }

    private fun normalizeErrorCode(value: String): String = value.trim().also {
        if (!ERROR_CODE.matches(it)) fail("AUTH_SECURITY_EVENT_NOTIFICATION_ERROR_CODE_INVALID")
    }

    private fun validateId(value: String) {
        if (value.isBlank() || value.length > 36 || value.any(Char::isISOControl)) {
            fail("AUTH_SECURITY_EVENT_NOTIFICATION_ID_INVALID")
        }
    }

    private fun fail(errorCode: String): Nothing = throw AuthSecurityEventException(errorCode)

    private companion object {
        const val MAX_CLAIM_LIMIT = 500
        const val MAX_ATTEMPTS = 100
        val WORKER_ID = Regex("^[A-Za-z0-9_.:-]{1,64}$")
        val ERROR_CODE = Regex("^[A-Za-z0-9_.:-]{1,64}$")
    }
}
