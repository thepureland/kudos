package io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDispatchResult

interface IAuthSecurityEventNotificationDispatcher {
    fun dispatchPending(workerId: String, limit: Int = 100): AuthSecurityEventNotificationDispatchResult
}
