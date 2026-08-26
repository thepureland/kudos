package io.kudos.ms.auth.core.authentication.securityevent.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Deduplicated, tenant-scoped authentication security event. */
interface AuthSecurityEvent : IDbEntity<String, AuthSecurityEvent> {
    companion object : DbEntityFactory<AuthSecurityEvent>()

    var tenantId: String
    var userId: String
    var eventType: String
    var subjectType: String
    var subjectFingerprint: String
    var riskLevel: String
    var riskSources: String?
    var riskStatusCodes: String?
    var deduplicationKey: String
    var bucketStart: LocalDateTime
    var status: String
    var workflowVersion: Long
    var occurrenceCount: Long
    var firstOccurredAt: LocalDateTime
    var lastOccurredAt: LocalDateTime
    var acknowledgedBy: String?
    var acknowledgedAt: LocalDateTime?
    var acknowledgeReason: String?
    var resolution: String?
    var closedBy: String?
    var closedAt: LocalDateTime?
    var closeReason: String?
    var assignedTo: String?
    var assignedBy: String?
    var assignedAt: LocalDateTime?
    var assignmentReason: String?
    var dueAt: LocalDateTime?
    var escalationLevel: Int
    var lastEscalatedAt: LocalDateTime?
    var nextEscalationAt: LocalDateTime?
    var createTime: LocalDateTime
    var updateTime: LocalDateTime
}
