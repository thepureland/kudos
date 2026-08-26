package io.kudos.ms.auth.core.authentication.securityevent.service.impl

import io.kudos.ms.auth.core.authentication.securityevent.dao.AuthSecurityEventDao
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.model.po.AuthSecurityEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.policy.IAuthSecurityEventEscalationPolicy
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventEscalationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
open class AuthSecurityEventEscalationService(
    private val eventDao: AuthSecurityEventDao,
    private val processor: AuthSecurityEventEscalationProcessor,
    private val clock: Clock = Clock.systemUTC(),
) : IAuthSecurityEventEscalationService {

    override fun scanDue(limit: Int): Int {
        if (limit !in 1..MAX_SCAN_LIMIT) throw AuthSecurityEventException("AUTH_SECURITY_EVENT_SCAN_LIMIT_INVALID")
        val now = LocalDateTime.now(clock)
        return eventDao.findEscalationDue(now, limit).count { processor.escalate(it) }
    }

    private companion object {
        const val MAX_SCAN_LIMIT = 500
    }
}

/** Separate bean so every candidate has an independent atomic event + outbox transaction. */
@Service
open class AuthSecurityEventEscalationProcessor(
    private val eventDao: AuthSecurityEventDao,
    private val notificationDao: AuthSecurityEventNotificationDao,
    private val policy: IAuthSecurityEventEscalationPolicy,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun escalate(event: AuthSecurityEvent): Boolean {
        val now = LocalDateTime.now(clock)
        val newLevel = event.escalationLevel + 1
        val nextEscalationAt = policy.nextEscalationAt(newLevel, now)
        if (!eventDao.escalate(event.id, event.escalationLevel, now, nextEscalationAt)) return false
        notificationDao.insert(AuthSecurityEventNotification {
            id = UUID.randomUUID().toString()
            tenantId = event.tenantId
            eventId = event.id
            notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED.name
            escalationLevel = newLevel
            recipientUserId = event.assignedTo
            dueAt = event.dueAt
            escalatedAt = now
            status = AuthSecurityEventNotificationStatusEnum.PENDING.name
            attemptCount = 0
            nextAttemptAt = now
            leaseOwner = null
            leaseUntil = null
            deliveredAt = null
            lastErrorCode = null
            replayCount = 0
            createTime = now
            updateTime = now
        })
        return true
    }
}
