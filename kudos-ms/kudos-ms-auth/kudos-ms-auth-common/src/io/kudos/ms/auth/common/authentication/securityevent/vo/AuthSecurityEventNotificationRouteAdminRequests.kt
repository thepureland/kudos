package io.kudos.ms.auth.common.authentication.securityevent.vo

import java.time.LocalDateTime

/**
 * Upsert of one tenant notification route rule.
 *
 * Deliberately without a tenant or an operator field: both are taken from the trusted administrator context,
 * and accepting them here would make cross-tenant writes a request-body away.
 */
data class AuthSecurityEventNotificationRouteAdminSaveRequest(
    val notificationType: String,
    val appliesTo: String,
    val routeCode: String,
    val destination: String,
    val channels: Set<String>,
    val responderUserIds: Set<String> = emptySet(),
    val responderRosterCode: String? = null,
    val includeAssignee: Boolean = false,
    val enabled: Boolean = true,
    val fallbackBehavior: String,
    /** `0` creates the rule; otherwise the `configVersion` the page was rendered from. */
    val expectedVersion: Long,
    val reason: String,
)

data class AuthSecurityEventNotificationRouteAdminResponse(
    val notificationType: String,
    val appliesTo: String,
    val routeCode: String,
    val destination: String,
    val channels: Set<String>,
    val responderUserIds: Set<String>,
    val responderRosterCode: String?,
    val includeAssignee: Boolean,
    val enabled: Boolean,
    val fallbackBehavior: String,
    val configVersion: Long,
    val createUserId: String,
    val createReason: String,
    val createTime: LocalDateTime,
    val updateUserId: String,
    val updateReason: String,
    val updateTime: LocalDateTime,
)
