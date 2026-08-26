package io.kudos.ms.auth.notification.eventbus

import java.io.Serializable
import java.time.LocalDateTime

/**
 * What leaves the service on the bus.
 *
 * Deliberately identifiers and the routing decision only: no credential material, no subject fingerprints, no
 * risk sources or status codes, no recipient contact details. A consumer that needs the incident's evidence
 * reads it back through the tenant-scoped admin API, where the permission check still applies — putting it on
 * a topic would hand it to everyone with bus access instead.
 *
 * [idempotencyKey] is supplied rather than left to the consumer to compose, because at-least-once delivery
 * makes deduplication the consumer's responsibility and the key must match the one a retry would produce.
 */
data class AuthSecurityEventBusNotificationMessage(
    val idempotencyKey: String,
    val notificationId: String,
    val tenantId: String,
    val eventId: String,
    val notificationType: String,
    val escalationLevel: Int,
    val attemptCount: Int,
    val dueAt: LocalDateTime?,
    val escalatedAt: LocalDateTime,
    val routeCode: String,
    val destination: String,
    val channel: String,
    val recipientUserIds: Set<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
