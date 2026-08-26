package io.kudos.ms.auth.core.authentication.securityevent.notification.routing

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import java.io.Serializable
import java.time.LocalDateTime

/**
 * Which delivery shape a stored route applies to.
 *
 * The outbox row either carries an assignee snapshot or it does not, and the two cases need different
 * destinations in practice, so they are configured as separate rows rather than one row with implicit branches.
 */
enum class AuthSecurityEventNotificationRouteAppliesToEnum {
    ASSIGNED,
    UNASSIGNED,
}

/** What happens when an enabled configuration cannot produce a deliverable recipient set. */
enum class AuthSecurityEventNotificationRouteFallbackEnum {
    /** Resolve through the built-in default policy, i.e. the pre-V53 behaviour. */
    DEFAULT_ROUTE,

    /** Degrade to the safe tenant security queue over the event bus. */
    TENANT_SECURITY_QUEUE,

    /** Refuse to guess: the dispatcher moves the notification to `DEAD` with an invalid-route code. */
    FAIL,
}

/**
 * One persisted tenant route rule.
 *
 * Cached across nodes, so it is JDK-serializable like the other cached auth value types.
 */
data class AuthSecurityEventNotificationRouteConfig(
    val tenantId: String,
    val notificationType: AuthSecurityEventNotificationTypeEnum,
    val appliesTo: AuthSecurityEventNotificationRouteAppliesToEnum,
    val routeCode: String,
    val destination: AuthSecurityEventNotificationDestinationEnum,
    val channels: Set<AuthSecurityEventNotificationChannelEnum>,
    val responderUserIds: Set<String>,
    /** Optional rotation whose current responders are merged into the recipients; `null` disables it. */
    val responderRosterCode: String?,
    val includeAssignee: Boolean,
    val enabled: Boolean,
    val fallbackBehavior: AuthSecurityEventNotificationRouteFallbackEnum,
    val configVersion: Long,
    val createUserId: String,
    val createReason: String,
    val createTime: LocalDateTime,
    val updateUserId: String,
    val updateReason: String,
    val updateTime: LocalDateTime,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * One appended change record.
 *
 * [beforeSnapshot] is `null` only for the first write of a rule; every later version keeps the value it
 * replaced, so a routing change can be reconstructed without relying on the mutable row.
 */
data class AuthSecurityEventNotificationRouteAuditRecord(
    val id: String,
    val tenantId: String,
    val routeId: String,
    val actorUserId: String,
    val reason: String,
    val configVersion: Long,
    val beforeSnapshot: String?,
    val afterSnapshot: String,
    val changedAt: LocalDateTime,
)

/**
 * An upsert of exactly one rule.
 *
 * [tenantId] and [actorUserId] are fixed by the caller from the trusted administrator context; they are never
 * taken from a request body. [expectedVersion] is `0` for a first write and the currently displayed
 * `configVersion` for an update.
 */
data class AuthSecurityEventNotificationRouteSaveCommand(
    val tenantId: String,
    val notificationType: String,
    val appliesTo: String,
    val routeCode: String,
    val destination: String,
    val channels: Set<String>,
    val responderUserIds: Set<String>,
    /** Optional rotation whose current responders are merged into the recipients; `null` disables it. */
    val responderRosterCode: String?,
    val includeAssignee: Boolean,
    val enabled: Boolean,
    val fallbackBehavior: String,
    val expectedVersion: Long,
    val actorUserId: String,
    val reason: String,
)
