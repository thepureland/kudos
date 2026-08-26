package io.kudos.ms.auth.core.authentication.securityevent.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.securityevent.model.po.AuthSecurityEvent
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthSecurityEvents : StringIdTable<AuthSecurityEvent>("auth_security_event") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var eventType = varchar("event_type").bindTo { it.eventType }
    var subjectType = varchar("subject_type").bindTo { it.subjectType }
    var subjectFingerprint = varchar("subject_fingerprint").bindTo { it.subjectFingerprint }
    var riskLevel = varchar("risk_level").bindTo { it.riskLevel }
    var riskSources = text("risk_sources").bindTo { it.riskSources }
    var riskStatusCodes = text("risk_status_codes").bindTo { it.riskStatusCodes }
    var deduplicationKey = varchar("deduplication_key").bindTo { it.deduplicationKey }
    var bucketStart = datetime("bucket_start").bindTo { it.bucketStart }
    var status = varchar("status").bindTo { it.status }
    var workflowVersion = long("workflow_version").bindTo { it.workflowVersion }
    var occurrenceCount = long("occurrence_count").bindTo { it.occurrenceCount }
    var firstOccurredAt = datetime("first_occurred_at").bindTo { it.firstOccurredAt }
    var lastOccurredAt = datetime("last_occurred_at").bindTo { it.lastOccurredAt }
    var acknowledgedBy = varchar("acknowledged_by").bindTo { it.acknowledgedBy }
    var acknowledgedAt = datetime("acknowledged_at").bindTo { it.acknowledgedAt }
    var acknowledgeReason = varchar("acknowledge_reason").bindTo { it.acknowledgeReason }
    var resolution = varchar("resolution").bindTo { it.resolution }
    var closedBy = varchar("closed_by").bindTo { it.closedBy }
    var closedAt = datetime("closed_at").bindTo { it.closedAt }
    var closeReason = varchar("close_reason").bindTo { it.closeReason }
    var assignedTo = varchar("assigned_to").bindTo { it.assignedTo }
    var assignedBy = varchar("assigned_by").bindTo { it.assignedBy }
    var assignedAt = datetime("assigned_at").bindTo { it.assignedAt }
    var assignmentReason = varchar("assignment_reason").bindTo { it.assignmentReason }
    var dueAt = datetime("due_at").bindTo { it.dueAt }
    var escalationLevel = int("escalation_level").bindTo { it.escalationLevel }
    var lastEscalatedAt = datetime("last_escalated_at").bindTo { it.lastEscalatedAt }
    var nextEscalationAt = datetime("next_escalation_at").bindTo { it.nextEscalationAt }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
