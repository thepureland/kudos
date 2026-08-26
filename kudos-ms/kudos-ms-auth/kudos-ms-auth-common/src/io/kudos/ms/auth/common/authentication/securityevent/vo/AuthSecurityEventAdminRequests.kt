package io.kudos.ms.auth.common.authentication.securityevent.vo

data class AuthSecurityEventAcknowledgeRequest(
    val expectedVersion: Long,
    val reason: String,
)

data class AuthSecurityEventCloseRequest(
    val expectedVersion: Long,
    val resolution: String,
    val reason: String,
)

data class AuthSecurityEventAssignRequest(
    val assigneeUserId: String,
    val expectedVersion: Long,
    val reason: String,
)

data class AuthSecurityEventNotificationReplayRequest(
    val reason: String,
)
