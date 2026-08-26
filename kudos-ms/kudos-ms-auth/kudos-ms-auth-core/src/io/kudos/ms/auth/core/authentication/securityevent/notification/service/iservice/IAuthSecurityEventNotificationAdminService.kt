package io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationSummary

interface IAuthSecurityEventNotificationAdminService {
    fun listDead(tenantId: String, eventId: String? = null, limit: Int = 100): List<AuthSecurityEventNotificationSummary>

    fun replay(command: AuthSecurityEventNotificationReplayCommand): AuthSecurityEventNotificationSummary
}
